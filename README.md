cameraMediaCodec
================

On Android platform, use MediaCodec to encode camera preview data and decode it to surface after that.


For now, encoder part(capture+encoder+preview) is OK, decoder part(decoder+render) is OK. The test tool is the first step of a real-time video application, like "Skype".

The data flow of encoder part is like that: get yuv data from camera preview callback, restore it into a queue(FIFO). In another thread, call QueueInputBuffer to handle the queue and call DequeueOutputBuffer to get Avc buffer. The process speed of DequeueOutputBuffer is related with QueueInputBuffer, vise versa. If QueueInputBuffer is slow, DequeueOutputBuffer will be slow.
The data flow of decoder part is: in a thread, parsing the AVC buffer. If SPS/PPS found, restart the decoder. If not, call QueueInputBuffer to take away AVC buffer, and also call DequeueOutputBuffer to get raw video data. 

For decoder part, need parse SPS/PPS ahead before calling configure, because we have to set "csd-0" and "csd-1" explicitly. Currently I call configure every time SPS/PPS with different picture size happens.

Make sure the preview callback return ASAP, for a high FPS. So don't handle input buffer directly in preview callback.
Make sure there is only one time buffer copy from camera to encoder.

There are two main features for a realtime application:
1. requested key frame
2. width/height/fps/bitrate config on the fly
Both support nearly perfect


New feature, but under testing:
1. support SVC encoder
