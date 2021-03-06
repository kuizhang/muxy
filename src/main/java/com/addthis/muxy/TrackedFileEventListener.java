/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.muxy;

/* tracks usage events */
class TrackedFileEventListener implements MuxyEventListener {

    TrackedMultiplexFileManager mfm;

    @Override
    public void streamEvent(MuxyStreamEvent event, Object target) {}

    @Override
    public void fileEvent(MuxyFileEvent event, Object target) {
        if (event == MuxyFileEvent.LOG_COMPACT) {
            mfm.releaseAfter(1000);
        }
    }

    void setTrackedInstance(TrackedMultiplexFileManager mfm) {
        this.mfm = mfm;
    }

    @Override
    public void reportWrite(long bytes) {
        mfm.cacheInstance.reportWrite(bytes);
    }

    @Override
    public void reportStreams(long streams) {
        mfm.cacheInstance.reportStreams(streams);
    }
}
