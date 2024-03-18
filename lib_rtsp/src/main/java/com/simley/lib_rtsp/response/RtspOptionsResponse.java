/*
 * Copyright 2021 The Android Open Source Project
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
package com.simley.lib_rtsp.response;

import com.google.common.collect.ImmutableList;
import com.simley.lib_rtsp.RtspRequest;

import java.util.List;

/** Represents an RTSP OPTIONS response. */
/* package */ final class RtspOptionsResponse {
  /** The response's status code. */
  public final int status;
  /**
   * A list of methods supported by the RTSP server, encoded as {@link RtspRequest.Method}; or an
   * empty list if the server does not disclose the supported methods.
   */
  public final ImmutableList<Integer> supportedMethods;

  /**
   * Creates a new instance.
   *
   * @param status The response's status code.
   * @param supportedMethods A list of methods supported by the RTSP server, encoded as {@link
   *     RtspRequest.Method}; or an empty list if such information is not available.
   */
  public RtspOptionsResponse(int status, List<Integer> supportedMethods) {
    this.status = status;
    this.supportedMethods = ImmutableList.copyOf(supportedMethods);
  }
}
