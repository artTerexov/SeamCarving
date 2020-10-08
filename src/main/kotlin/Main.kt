package seamcarving

import java.io.File

fun main(args: Array<String>) {

    val fileIn = File(args[1])
    val fileOut = File(args[3])
    val width = args[5].toInt()
    val height= args[7].toInt()

    val seamCarving = SeamCarving(fileIn, fileOut, width, height)
    seamCarving.cutImage()
}


