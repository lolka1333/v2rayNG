package com.v2ray.ang.ui

interface BaseAdapterListener {
    fun onEdit(guid: String)
    fun onRemove(guid: String, position: Int)
    fun onShare(url: String)
    fun onRefreshData()
}
