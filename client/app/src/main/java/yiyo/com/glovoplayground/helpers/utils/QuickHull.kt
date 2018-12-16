package yiyo.com.glovoplayground.helpers.utils

import com.google.android.gms.maps.model.LatLng

/*
 * Copyright (c) 2017 Kotlin Algorithm Club
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
object QuickHull {
    fun convexHull(points: List<LatLng>): Collection<LatLng> {
        if (points.size < 3) throw IllegalArgumentException("there must be at least 3 points")
        val left = points.minBy { it.latitude }!!
        val right = points.maxBy { it.latitude }!!
        return quickHull(
            points,
            left,
            right
        ) + quickHull(points, right, left)
    }

    private fun quickHull(points: Collection<LatLng>, first: LatLng, second: LatLng): Collection<LatLng> {
        val pointsLeftOfLine = points
            .filter { it.isLeftOfLine(first, second) }
            .map { Pair(it, it.distanceToLine(first, second)) }

        return if (pointsLeftOfLine.isEmpty()) {
            listOf(second)
        } else {
            val max = pointsLeftOfLine.maxBy { it.second }!!.first
            val newPoints = pointsLeftOfLine.map { it.first }
            quickHull(
                newPoints,
                first,
                max
            ) + quickHull(newPoints, max, second)
        }
    }

    private fun LatLng.isLeftOfLine(from: LatLng, to: LatLng): Boolean {
        return crossProduct(from, to) > 0
    }

    private fun LatLng.crossProduct(origin: LatLng, p2: LatLng): Double {
        return (p2.latitude - origin.latitude) * (this.longitude - origin.longitude) -
                (p2.longitude - origin.longitude) * (this.latitude - origin.latitude)
    }

    private fun LatLng.distanceToLine(a: LatLng, b: LatLng): Double {
        return Math.abs(
            (b.latitude - a.latitude) * (a.longitude - this.longitude) - (a.latitude - this.latitude) * (b.longitude - a.longitude)
        ) / Math.sqrt(Math.pow((b.latitude - a.latitude), 2.0) + Math.pow((b.longitude - a.longitude), 2.0))
    }
}