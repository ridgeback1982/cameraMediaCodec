cameraMediaCodec
================

On Android platform, use MediaCodec to encode camera preview data and decode it to surface after that.


For now, only the encoder part is finished. Decoder part is pending.
The data flow is like that: get yuv data from camera preview callback, do not crop or resample the raw data, put it directly to MediaCodec by QueueInputBuffer call. In another thread, call DequeueOutputBuffer to get Avc buffer, because dequeue is always in waiting.The process speed of DequeueOutputBuffer is related with QueueInputBuffer. If QueueInputBuffer is slow, DequeueOutputBuffer will be slow. Make sure the QueueInputBuffer is never blocked(cause it is in preview callback).

There are two main features for a realtime application:
1. requested key frame
2. width/height/fps/bitrate config on the fly
