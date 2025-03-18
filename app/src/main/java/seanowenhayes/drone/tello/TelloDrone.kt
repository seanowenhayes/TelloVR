package seanowenhayes.drone.tello

import seanowenhayes.drone.generic.Control
import seanowenhayes.drone.generic.Video

class TelloDrone: Control, Video {

    companion object {
        const val UDP_PORT: Int = 8889
        const val UDP_STATUS_PORT: Int = 8890
        const val UDP_VIDEO_PORT: Int = 11111
        const val IP_ADDRESS: String = "192.168.10.1"
    }

    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun takeoff() {
        TODO("Not yet implemented")
    }

    override fun land() {
        TODO("Not yet implemented")
    }

    override fun fly(lr: Int, fb: Int, ud: Int, yaw: Int) {
        TODO("Not yet implemented")
    }
}