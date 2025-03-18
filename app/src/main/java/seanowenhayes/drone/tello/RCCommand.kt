package seanowenhayes.drone.tello

import seanowenhayes.drone.generic.Command


data class RCCommand(val lr: Int, val fb: Int, val ud: Int, val yaw: Int): Command {
    override fun composeCommand(): String {
        return "rc $lr $fb $ud $yaw"
    }
}