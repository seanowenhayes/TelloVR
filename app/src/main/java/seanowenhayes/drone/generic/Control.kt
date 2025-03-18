package seanowenhayes.drone.generic

interface Control {

    fun connect()

    fun disconnect()

    fun takeoff()

    fun land()

    /**
     * Fly by remote control. Units are speed in cm/s.
     * @param lr Left/Right (-100 to 100).
     * @param fb forward/backward (-100 to 100).
     * @param ud up/down (-100 to 100).
     * @param yaw yaw value in degrees.
     */
    fun fly(lr: Int, fb: Int, ud: Int, yaw: Int)
}