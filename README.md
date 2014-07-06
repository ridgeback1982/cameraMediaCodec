cameraMediaCodec
================

On Android platform, use MediaCodec to encode camera preview data and decode it to surface after that.


For now, only the encoder part is finished. Decoder part is under testing.

The data flow is like that: get yuv data from camera preview callback, restore it into a queue(FIFO). In another thread, call QueueInputBuffer to handle the queue and call DequeueOutputBuffer to get Avc buffer. The process speed of DequeueOutputBuffer is related with QueueInputBuffer, vise versa. If QueueInputBuffer is slow, DequeueOutputBuffer will be slow.

For decoder part, need parse SPS/PPS ahead before calling configure, because we have to set "csd-0" and "csd-1" explicitly.
If AVC picture size changed, currently we don't need to call configure again if the new picture is smaller than the previous, it is cool :-)

Make sure the preview callback return ASAP, for a high FPS. So don't handle input buffer directly in preview callback.
Make sure there is only one time buffer copy from camera to encoder.

There are two main features for a realtime application:
1. requested key frame
2. width/height/fps/bitrate config on the fly
Both support nearly perfect
