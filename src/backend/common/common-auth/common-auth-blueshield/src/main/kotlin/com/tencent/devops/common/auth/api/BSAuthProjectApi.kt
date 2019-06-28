/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.api.pojo.BkAuthProjectCodeAndId
import com.tencent.devops.common.auth.api.pojo.BkAuthResponse
import com.tencent.devops.common.auth.code.AuthServiceCode
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BSAuthProjectApi @Autowired constructor(
    private val bkAuthProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper,
    private val bsAuthTokenApi: BSAuthTokenApi,
    private val bsCCProjectApi: BSCCProjectApi
) : AuthProjectApi {

    override fun isProjectUser(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        group: BkAuthGroup?
    ): Boolean {
        return getProjectUsers(serviceCode, projectCode, group).contains(user)
    }

    override fun getProjectUsers(serviceCode: AuthServiceCode, projectCode: String, group: BkAuthGroup?): List<String> {
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = if (group == null) {
            "${bkAuthProperties.url}/projects/$projectCode/users?access_token=$accessToken"
        } else {
            "${bkAuthProperties.url}/projects/$projectCode/users?access_token=$accessToken&group_code=${group.value}"
        }

        val request = Request.Builder().url(url).get().build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get project users. $responseContent")
                throw RemoteServiceException("Fail to get project users")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<List<String>>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.error("Fail to get project users. $responseContent")
                throw RemoteServiceException("Fail to get project users")
            }
            return responseObject.data ?: emptyList()
        }
    }

    override fun getProjectGroupAndUserList(
        serviceCode: AuthServiceCode,
        projectCode: String
    ): List<BkAuthGroupAndUserList> {
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/projects/$projectCode/roles/?access_token=$accessToken&fields=user_list"
        val request = Request.Builder().url(url).get().build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get project group and user list. $responseContent")
                throw RemoteServiceException("Fail to get project group and user list")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<List<BkAuthGroupAndUserList>>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.error("Fail to get project group and user list. $responseContent")
                throw RemoteServiceException("Fail to get project group and user list")
            }
            return responseObject.data ?: emptyList()
        }
    }

    override fun getUserProjects(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): List<String> {
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/projects?access_token=$accessToken&user_id=$userId"
        val request = Request.Builder().url(url).get().build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get user projects. $responseContent")
                throw RemoteServiceException("Fail to get user projects")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<List<BkAuthProjectCodeAndId>>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.error("Fail to get user projects. $responseContent")
                throw RemoteServiceException("Fail to get user projects")
            }
            val projectCodeAndIdList = responseObject.data ?: emptyList()
            return projectCodeAndIdList.map { it.projectCode }
        }
    }

    override fun getUserProjectsAvailable(
        serviceCode: AuthServiceCode,
        userId: String,
        supplier: (() -> List<String>)?
    ): Map<String, String> {
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/projects?access_token=$accessToken&user_id=$userId"
        val request = Request.Builder().url(url).get().build()
//
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get user projects. $responseContent")
                throw RemoteServiceException("Fail to get user projects")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<List<BkAuthProjectCodeAndId>>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.error("Fail to get user projects. $responseContent")
                throw RemoteServiceException("Fail to get user projects")
            }
            val projectCodeAndIdList = responseObject.data ?: emptyList()
            val projecrCodeList = projectCodeAndIdList.map { it.projectCode }.toSet()
            val projectAvailableList = mutableMapOf<String, String>()
            val bkProjectInfos =
                bsCCProjectApi.getProjectListAsOuter(projecrCodeList).map { it.projectCode to it }.toMap()
            projectCodeAndIdList.forEach {
                val projectInfo = bkProjectInfos[it.projectCode] ?: return@forEach

                // 只有审批通过的项目才返回
                if (projectInfo.approvalStatus != "2" && projectInfo.approvalStatus != "1") {
                    return@forEach
                }
                // 只有未禁用的项目才返回
                if (projectInfo.isOfflined) {
                    return@forEach
                }
                projectAvailableList[it.projectCode] = projectInfo.projectName
            }
            return projectAvailableList
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BSAuthProjectApi::class.java)
        private const val HTTP_403 = 403
    }
}