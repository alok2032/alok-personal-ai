package com.ayra.ai.app.automation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ResolvedContact(
    val name: String,
    val phoneNumber: String
)

object ContactResolver {

    /**
     * Resolves a contact name into a phone number via ContactsContract.
     * Returns null if contact not found or permission not granted.
     */
    fun resolveContact(context: Context, queryName: String): ResolvedContact? {
        val cleanQuery = queryName.trim()
        if (cleanQuery.isEmpty()) return null

        // If query is already a valid phone number, return directly
        if (cleanQuery.matches(Regex("\\+?[0-9]{3,15}"))) {
            return ResolvedContact(name = cleanQuery, phoneNumber = cleanQuery)
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return null

        return try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$cleanQuery%")

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name = if (nameIndex >= 0) it.getString(nameIndex) ?: cleanQuery else cleanQuery
                    val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                    if (number.isNotBlank()) {
                        return ResolvedContact(name = name, phoneNumber = number)
                    }
                }
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("ContactResolver", "Failed to query contacts", e)
            null
        }
    }
}
