package com.shacha.picword

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.luck.picture.lib.engine.ImageEngine
import com.luck.picture.lib.helper.ActivityCompatHelper.assertValidRequest

object GlideEngine : ImageEngine {
    override fun loadImage(
        context: Context,
        url: String?,
        imageView: ImageView
    ) {
        if (!assertValidRequest(context)) {
            return
        }
        Glide.with(context)
            .load(url)
            .into(imageView)
    }

    override fun loadImage(
        context: Context,
        url: String?,
        width: Int,
        height: Int,
        imageView: ImageView
    ) {
        if (!assertValidRequest(context)) {
            return
        }
        Glide.with(context)
            .load(url)
            .override(width, height)
            .into(imageView)
    }


    override fun loadAlbumCover(context: Context, url: String?, imageView: ImageView) {
        if (!assertValidRequest(context)) {
            return
        }
        Glide.with(context)
            .asBitmap()
            .load(url)
            .override(180, 180)
            .sizeMultiplier(0.5f)
            .transform(CenterCrop(), RoundedCorners(8))
            .into(imageView)
    }


    override fun loadListImage(context: Context, url: String?, imageView: ImageView) {
        if (!assertValidRequest(context)) {
            return
        }
        Glide.with(context)
            .load(url)
            .override(200, 200)
            .centerCrop()
            .into(imageView)
    }

    override fun pauseRequests(context: Context) {
        if (!assertValidRequest(context)) {
            return
        }
        Glide.with(context).pauseRequests()
    }

    override fun resumeRequests(context: Context) {
        if (!assertValidRequest(context)) {
            return
        }
        Glide.with(context).resumeRequests()
    }
}