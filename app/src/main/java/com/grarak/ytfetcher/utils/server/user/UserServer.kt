package com.grarak.ytfetcher.utils.server.user

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.grarak.ytfetcher.utils.Settings
import com.grarak.ytfetcher.utils.server.GenericCallback
import com.grarak.ytfetcher.utils.server.Request
import com.grarak.ytfetcher.utils.server.Server
import com.grarak.ytfetcher.utils.server.Status
import java.net.HttpURLConnection

class UserServer(url: String) : Server(url) {

    interface UserCallback {
        fun onSuccess(user: User)

        fun onFailure(code: Int)
    }

    interface UsersCallback {
        fun onSuccess(users: List<User>)

        fun onFailure(code: Int)
    }

    constructor(context: Context) : this(Settings.getServerUrl(context)) {}

    fun signUp(user: User, userCallback: UserCallback) {
        post(getUrl("users/signup"), user.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                handleUserCallbackSuccess(userCallback, status, response)
            }

            override fun onFailure(request: Request, e: Exception?) {
                handleUserCallbackFailure(userCallback)
            }
        })
    }

    fun login(user: User, userCallback: UserCallback) {
        post(getUrl("users/login"), user.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                handleUserCallbackSuccess(userCallback, status, response)
            }

            override fun onFailure(request: Request, e: Exception?) {
                handleUserCallbackFailure(userCallback)
            }
        })
    }

    fun list(user: User, page: Int, usersCallback: UsersCallback) {
        post(getUrl("users/list?page=$page"), user.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    val listType = object : TypeToken<List<User>>() {

                    }.type
                    val results = GsonBuilder().create().fromJson<List<User>>(response, listType)
                    usersCallback.onSuccess(results)
                } else {
                    usersCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                usersCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    fun setVerification(user: User, genericCallback: GenericCallback) {
        post(getUrl("users/setverification"), user.toString(), object : Request.RequestCallback {
            override fun onConnect(request: Request, status: Int, url: String) {}

            override fun onSuccess(request: Request, status: Int,
                                   headers: Map<String, List<String>>, response: String) {
                if (status == HttpURLConnection.HTTP_OK) {
                    genericCallback.onSuccess()
                } else {
                    genericCallback.onFailure(parseStatusCode(response))
                }
            }

            override fun onFailure(request: Request, e: Exception?) {
                genericCallback.onFailure(Status.ServerOffline)
            }
        })
    }

    private fun handleUserCallbackSuccess(userCallback: UserCallback,
                                          status: Int, response: String) {
        if (status == HttpURLConnection.HTTP_OK) {
            val user = User.fromString(response)
            if (user != null) {
                userCallback.onSuccess(user)
            } else {
                userCallback.onFailure(Status.ServerOffline)
            }
        } else {
            userCallback.onFailure(parseStatusCode(response))
        }
    }

    private fun handleUserCallbackFailure(userCallback: UserCallback) {
        userCallback.onFailure(Status.ServerOffline)
    }
}
