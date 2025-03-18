package seanowenhayes.drone.tello

class TelloConnectionException(message: String, error: Error): Exception(message, error)

class TelloCommandException(message: String): Exception(message)