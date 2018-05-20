package com.grarak.ytfetcher.utils.server

import com.google.gson.GsonBuilder

import java.io.Serializable

abstract class Gson : Serializable {

    override fun toString(): String {
        return GsonBuilder().create().toJson(this)
    }

    override fun equals(other: Any?): Boolean {
        return other is Gson && toString() == other.toString()
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
