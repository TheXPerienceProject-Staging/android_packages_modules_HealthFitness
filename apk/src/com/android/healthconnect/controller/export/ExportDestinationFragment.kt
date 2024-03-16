/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.healthconnect.controller.export

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import dagger.hilt.android.AndroidEntryPoint

/** Export destination fragment for Health Connect. */
@AndroidEntryPoint(Fragment::class)
class ExportDestinationFragment : Hilt_ExportDestinationFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.export_destination_screen, container, false)
        // TODO: b/325917283 - Add proper navigation to the next screen (either file picker or
        // document UI).
        val backButton = view.findViewById<Button>(R.id.export_back_button)

        backButton.setOnClickListener {
            findNavController()
                .navigate(R.id.action_exportDestinationFragment_to_exportFrequencyFragment)
        }
        return view
    }
}