package space.dector.tuyalib


internal sealed class Result<T> {
    internal data class Ok<T>(val value: T) : Result<T>()
    internal data class Fail<T>(val err: Throwable) : Result<T>()
}
