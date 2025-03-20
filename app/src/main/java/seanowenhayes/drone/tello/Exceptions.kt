package seanowenhayes.drone.tello

class TelloConnectionException : Exception {
    constructor(message: String, exception: Exception) : super(message, exception)
    constructor(message: String): super(message)
    constructor(error: Error): super(error)
    constructor(exception: Exception): super(exception)
}

class TelloCommandException : Exception {
    constructor(message: String, error: Error) : super(message, error)
    constructor(message: String): super(message)
    constructor(error: Error): super(error)
    constructor(exception: Exception): super(exception)
}