/*
SPDX-FileCopyrightText: 2021 MoleMan1024 <moleman1024dev@gmail.com>
SPDX-License-Identifier: GPL-3.0-or-later
*/

package de.moleman1024.audiowagon.medialibrary

import android.net.Uri
import de.moleman1024.audiowagon.Util
import de.moleman1024.audiowagon.filestorage.*
import de.moleman1024.audiowagon.log.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import java.nio.file.Paths

private val logger = Logger
private const val TAG = "PlaylistFileResolver"
private const val UTF8_BOM = "\uFEFF"
private val WINDOWS_ROOT_BACKSLASH_REGEX = Regex("^([A-Za-z]:)?\\\\")
private val WINDOWS_SEPARATORS_REGEX = Regex("\\\\(?=\\S)")
private val PLS_FILE_PREFIX = Regex("^File[0-9]+=")
private val XSPF_PREFIX = Regex("^file:///[A-Za-z]:")
private val IP_PREFIX = Regex("^(https?|rtsp)://")

@ExperimentalCoroutinesApi
class PlaylistFileResolver(
    private val playlistFileUri: Uri,
    private val audioFileStorage: AudioFileStorage
) {
    private var lockableInputStream: LockableInputStream

    init {
        runBlocking {
            lockableInputStream = audioFileStorage.getInputStream(playlistFileUri)
        }
    }

    fun parseForAudioItems(): List<AudioItem> {
        val audioItems: List<AudioItem>
        val playlistType = determineType(playlistFileUri) ?: return listOf()
        try {
            audioItems = when (playlistType) {
                PlaylistType.M3U -> {
                    parseM3U()
                }
                PlaylistType.PLS -> {
                    parsePLS()
                }
                PlaylistType.XSPF -> {
                    parseXSPF()
                }
            }
        } catch (exc: FileNotFoundException) {
            logger.exception(TAG, exc.message.toString(), exc)
            return listOf()
        }
        return audioItems
    }

    private fun parseM3U(): MutableList<AudioItem> {
        logger.debug(TAG, "Parsing .m3u playlist file")
        val audioItems = mutableListOf<AudioItem>()
        for (line in getLines()) {
            var lineSanitized = line
            if (lineSanitized.startsWith("#") || IP_PREFIX.find(lineSanitized) != null) {
                continue
            }
            lineSanitized = convertWindowsPathToLinuxPath(lineSanitized)
            val pathInPlaylist = Paths.get(lineSanitized)
            val audioItem: AudioItem = if (pathInPlaylist.isAbsolute) {
                convertPathToAudioItem(pathInPlaylist.toString())
            } else {
                val parentDir = Util.getParentPath(GeneralFile(playlistFileUri).path)
                convertPathToAudioItem(Paths.get("$parentDir/$pathInPlaylist").normalize().toString())
            }
            audioItems.add(audioItem)
        }
        return audioItems
    }

    private fun parsePLS(): MutableList<AudioItem> {
        logger.debug(TAG, "Parsing .pls playlist file")
        val audioItems = mutableListOf<AudioItem>()
        for (line in getLines()) {
            var lineSanitized = removeBOM(line.trim())
            if (PLS_FILE_PREFIX.find(lineSanitized) == null) {
                continue
            }
            lineSanitized = lineSanitized.replace(PLS_FILE_PREFIX, "")
            lineSanitized = convertWindowsPathToLinuxPath(lineSanitized)
            val pathInPlaylist = Paths.get(lineSanitized)
            val audioItem = convertPathToAudioItem(pathInPlaylist.toString())
            audioItems.add(audioItem)
        }
        return audioItems
    }

    private fun parseXSPF(): MutableList<AudioItem> {
        logger.debug(TAG, "Parsing .xspf playlist file")
        val audioItems = mutableListOf<AudioItem>()
        lockableInputStream.libaumsDispatcher?.let {
            runBlocking(it) {
                try {
                    val parserFactory = XmlPullParserFactory.newInstance()
                    val parser: XmlPullParser = parserFactory.newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(lockableInputStream.inputStream, null)
                    var tag: String?
                    var text = ""
                    var xmlEvent = parser.eventType
                    while (xmlEvent != XmlPullParser.END_DOCUMENT) {
                        tag = parser.name
                        when (xmlEvent) {
                            XmlPullParser.TEXT -> {
                                text = parser.text
                            }
                            XmlPullParser.END_TAG -> {
                                if (tag == "location") {
                                    val filePath = XSPF_PREFIX.replace(Uri.decode(text), "")
                                    val pathInPlaylist = Paths.get(filePath)
                                    val audioItem = convertPathToAudioItem(pathInPlaylist.toString())
                                    audioItems.add(audioItem)
                                }
                            }
                        }
                        xmlEvent = parser.next()
                    }
                } catch (exc: XmlPullParserException) {
                    logger.exception(TAG, exc.message.toString(), exc)
                }
            }
        }
        return audioItems
    }

    private fun convertPathToAudioItem(path: String): AudioItem {
        logger.debug(TAG, "Converted path: $path")
        val uri = Util.createURIForPath(audioFileStorage.getPrimaryStorageLocation().storageID, path)
        val audioFile = AudioFile(uri)
        return AudioItemLibrary.createAudioItemForFile(audioFile)
    }

    private fun removeBOM(line: String): String {
        if (line.startsWith(UTF8_BOM)) {
            return line.substring(1)
        }
        return line
    }

    private fun convertWindowsPathToLinuxPath(path: String): String {
        var pathConverted: String = path
        if (WINDOWS_ROOT_BACKSLASH_REGEX.find(pathConverted) != null) {
            pathConverted = pathConverted.replace(WINDOWS_ROOT_BACKSLASH_REGEX, "/")
        }
        pathConverted = pathConverted.replace(WINDOWS_SEPARATORS_REGEX, "/")
        return pathConverted
    }

    private fun getLines(): List<String> {
        val lines: MutableList<String> = mutableListOf()
        lockableInputStream.libaumsDispatcher?.let { dispatcher ->
            runBlocking(dispatcher) {
                try {
                    BufferedReader(InputStreamReader(lockableInputStream.inputStream)).use { br ->
                        var line: String?
                        while (br.readLine().also { line = it } != null) {
                            line?.let {
                                logger.debug(TAG, "Playlist content: $it")
                                val lineSanitized = removeBOM(it.trim())
                                if (lineSanitized.isNotBlank()) {
                                    lines.add(lineSanitized)
                                }
                            }
                            yield()
                        }
                    }
                } catch (exc: IOException) {
                    logger.exception(TAG, exc.message.toString(), exc)
                    lines.clear()
                    return@runBlocking
                }
            }
        }
        return lines
    }

    private fun determineType(uri: Uri): PlaylistType? {
        val playlistFile = PlaylistFile(uri)
        val guessedContentType = Util.guessContentType(playlistFile.name) ?: return null
        val playlistType: PlaylistType?
        try {
            playlistType = PlaylistType.fromMimeType(guessedContentType)
        } catch(exc: IllegalArgumentException) {
            logger.exception(TAG, exc.message.toString(), exc)
            return null
        }
        return playlistType
    }

}
