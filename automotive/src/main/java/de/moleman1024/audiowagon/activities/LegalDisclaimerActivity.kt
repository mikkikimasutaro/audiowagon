/*
SPDX-FileCopyrightText: 2021 MoleMan1024 <moleman1024dev@gmail.com>
SPDX-License-Identifier: GPL-3.0-or-later
*/

package de.moleman1024.audiowagon.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import de.moleman1024.audiowagon.R
import de.moleman1024.audiowagon.Util
import de.moleman1024.audiowagon.filestorage.usb.ACTION_USB_UPDATE
import de.moleman1024.audiowagon.log.Logger

private const val TAG = "LegalDisclAct"
private val logger = Logger
const val PERSISTENT_STORAGE_LEGAL_DISCLAIMER_AGREED = "agreedLegalVersion"
const val PERSISTENT_STORAGE_LEGAL_DISCLAIMER_VERSION = "1.0"

class LegalDisclaimerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.debug(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal_disclaimer)
        val toolbar = findViewById<Toolbar>(R.id.legalDisclaimerToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(R.drawable.baseline_west_24)
        val agreeButton = findViewById<Button>(R.id.legalDisclaimerAgreeBtn)
        agreeButton.setOnClickListener { onAgree() }
        val cancelButton = findViewById<Button>(R.id.legalDisclaimerCancelBtn)
        cancelButton.setOnClickListener { onCancel() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onCancel()
        return super.onSupportNavigateUp()
    }

    private fun onAgree() {
        logger.debug(TAG, "User agreed to legal disclaimer")
        if (Util.isLegalDisclaimerAgreed(this)) {
            // already agreed previously, do nothing
            finish()
            return
        }
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.edit()
            .putString(PERSISTENT_STORAGE_LEGAL_DISCLAIMER_AGREED, PERSISTENT_STORAGE_LEGAL_DISCLAIMER_VERSION).apply()
        val updateUSBIntent = Intent(ACTION_USB_UPDATE)
        sendBroadcast(updateUSBIntent)
        finish()
    }

    private fun onCancel() {
        logger.debug(TAG, "User cancelled")
        finish()
    }

    override fun onDestroy() {
        logger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }
}