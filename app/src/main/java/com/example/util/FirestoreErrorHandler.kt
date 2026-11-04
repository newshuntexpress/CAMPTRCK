package com.example.util

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    // If the user is unauthenticated or signing out, do not log a Firestore error
    if (currentUser == null) {
        Log.d("FirestoreAuth", "Firestore operation '${operationType.value}' on path '$path' occurred while unauthenticated: ${exception.message}")
        return ""
    }

    val providerInfoList = currentUser.providerData.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    }

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser.uid)
        put("email", currentUser.email)
        put("emailVerified", currentUser.isEmailVerified)
        put("tenantId", currentUser.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}
