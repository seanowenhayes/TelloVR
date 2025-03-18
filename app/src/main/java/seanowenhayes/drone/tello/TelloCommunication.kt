package seanowenhayes.drone.tello

import seanowenhayes.drone.generic.Command
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.logging.Logger

/**
 * Low level support interface for sending and receiving data from Tello drone.
 */
object TelloCommunication {
    private val logger: Logger = Logger.getLogger("Tello")

    /**
     * Datagram sockets for UDP communication with the Tello drone.
     */
    private var ds: DatagramSocket? = null
    private var dsStatus: DatagramSocket? = null

    /**
     * Drone IP address.
     */
    private var ipAddress: InetAddress? = null

    /**
     * Drone UDP ports and timeout.
     */
    private var udpPort: Int? = null
    private var udpStatusPort: Int? = null
    var timeout: Int = 10000

    // Private constructor, holder class and getInstance() implement this
    // class as a singleton.
    init {
        try {
            ipAddress = InetAddress.getByName(TelloDrone.IP_ADDRESS)
            udpPort = TelloDrone.UDP_PORT
            udpStatusPort = TelloDrone.UDP_STATUS_PORT
        } catch (e: Exception) {
            throw TelloConnectionException(e)
        }
    }

    @Throws(TelloConnectionException::class)
    fun connect() {
        try {
            logger.info("Connecting to drone...")

            ds = DatagramSocket(udpPort!!) // new dg socket to send/receive commands.

            ds!!.soTimeout = timeout // timeout on socket operations.

            ds!!.connect(ipAddress, udpPort!!)

            if (!ipAddress!!.isReachable(100)) throw TelloConnectionException("Tello not responding")

            dsStatus = DatagramSocket(udpStatusPort!!) // new dg socket to receive status feed.

            dsStatus!!.soTimeout = timeout // timeout on socket operations.

            logger.info("Connected!")
        } catch (e: Exception) {
            if (dsStatus != null) dsStatus!!.close()
            if (ds != null) ds!!.close()
            //e.printStackTrace();
            throw TelloConnectionException("Connect failed", e)
        }
    }

    @Synchronized
    @Throws(TelloConnectionException::class, TelloCommandException::class)
    fun executeCommand(telloCommand: TelloCommand) {
        val response: String

        if (!ds!!.isConnected) throw TelloConnectionException("No connection")

        val command: String = telloCommand.composeCommand()

        logger.fine("executing command: $command")

        try {
            sendData(command)
            response = receiveData()
        } catch (e: Exception) {
            throw TelloConnectionException(e)
        }

        logger.finer("response: $response")

        if (response.lowercase(Locale.getDefault()).startsWith("forced stop")) return
        if (response.lowercase(Locale.getDefault())
                .startsWith("unknown command")
        ) throw TelloCommandException("unknown command")
        if (response.lowercase(Locale.getDefault())
                .startsWith("out of range")
        ) throw TelloCommandException("invalid parameter")
        if (!response.lowercase(Locale.getDefault()).startsWith("ok")) throw TelloCommandException(
            "command failed: $response"
        )
    }

    @Synchronized
    @Throws(TelloConnectionException::class, TelloCommandException::class)
    fun executeCommandNoWait(telloCommand: Command) {

        if (!ds!!.isConnected) throw TelloConnectionException("No connection")

        val command: String = telloCommand.composeCommand()

        logger.finer("executing command: $command")

        try {
            sendData(command)
        } catch (e: Exception) {
            throw TelloConnectionException(e.toString())
        }
    }

    fun disconnect() {
        if (dsStatus != null) dsStatus!!.close()

        if (ds != null) ds!!.close()

        logger.info("Disconnected!")
    }

    @Throws(IOException::class)
    private fun sendData(data: String) {
        val sendData = data.toByteArray()
        val sendPacket = DatagramPacket(
            sendData, sendData.size, ipAddress,
            udpPort!!
        )
        ds!!.send(sendPacket)
    }

    @Throws(IOException::class)
    private fun receiveData(): String {
        val receiveData = ByteArray(1024)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)
        ds!!.receive(receivePacket)
        return trimExecutionResponse(receiveData, receivePacket)
    }

    @Throws(IOException::class)
    fun receiveStatusData(): String {
        val receiveData = ByteArray(1024)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)
        dsStatus!!.receive(receivePacket)
        return trimExecutionResponse(receiveData, receivePacket)
    }

    private fun trimExecutionResponse(response: ByteArray, receivePacket: DatagramPacket): String {
        var response = response
        response = response.copyOf(receivePacket.length)
        return String(response, StandardCharsets.UTF_8)
    }

    companion object {
    }
}