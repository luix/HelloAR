/*
 * Copyright 2020 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mx.luix.arjesus.common.rendering

import android.opengl.GLES20
import android.opengl.GLES30
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException

/** Handle the creation and update of a GPU texture.  */
class Texture {
    // Stores the latest provided texture id.
    var textureId = -1
        private set
    var width = -1
        private set
    var height = -1
        private set

    /**
     * Creates and initializes the texture. This method needs to be called on a thread with a EGL
     * context attached.
     */
    fun createOnGlThread() {
        val textureIdArray = IntArray(1)
        GLES20.glGenTextures(1, textureIdArray, 0)
        textureId = textureIdArray[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    /**
     * Updates the texture with the content from acquireDepthImage, which provides an image in DEPTH16
     * format, representing each pixel as a depth measurement in millimeters. This method needs to be
     * called on a thread with a EGL context attached.
     */
    fun updateWithDepthImageOnGlThread(frame: Frame) {
        try {
            val depthImage = frame.acquireDepthImage()
            width = depthImage.width
            height = depthImage.height
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES30.GL_RG8,
                width,
                height,
                0,
                GLES30.GL_RG,
                GLES20.GL_UNSIGNED_BYTE,
                depthImage.planes[0].buffer
            )
            depthImage.close()
        } catch (e: NotYetAvailableException) {
            // This normally means that depth data is not available yet. This is normal so we will not
            // spam the logcat with this.
        }
    }

}