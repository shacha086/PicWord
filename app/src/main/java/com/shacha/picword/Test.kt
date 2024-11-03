import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

suspend fun main() {
    (1..5).asFlow().map {
        if (it == 5) throw CancellationException()
        else it
    }.catch {
        this.emit(-1)
    }.collect{
        println(it)
    }
}