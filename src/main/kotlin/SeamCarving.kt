package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.sqrt

class SeamCarving(fileIn: File, private val fileOut: File, private val width: Int, private val height: Int) {

    private var bufferedImage: BufferedImage
    private lateinit var resizeImage: BufferedImage
    private lateinit var energyMatrix: Array<Array<Double>>

    init {
        bufferedImage = ImageIO.read(fileIn)!!
    }

    private data class DijkstrasPath(val previous: Pair<Int, Int>?, val energy: Double?)

    private fun dx2(x: Int, y: Int): Int {
        return when (x) {
            0 -> {
                dx2(x + 1, y)
            }
            bufferedImage.width - 1 -> {
                dx2(x - 1, y)
            }
            else -> {
                val l = Color(bufferedImage.getRGB(x - 1, y))
                val r = Color(bufferedImage.getRGB(x + 1, y))
                (l.red - r.red) * (l.red - r.red) + (l.green - r.green) * (l.green - r.green) + (l.blue - r.blue) * (l.blue - r.blue)
            }
        }
    }

    private fun dy2(x: Int, y: Int): Int {
        return when (y) {
            0 -> {
                dy2(x, y + 1)
            }
            bufferedImage.height - 1 -> {
                dy2(x, y - 1)
            }
            else -> {
                val t = Color(bufferedImage.getRGB(x, y - 1))
                val b = Color(bufferedImage.getRGB(x, y + 1))
                (t.red - b.red) * (t.red - b.red) + (t.green - b.green) * (t.green - b.green) + (t.blue - b.blue) * (t.blue - b.blue)
            }
        }
    }

    private fun pixelEnergy(x: Int, y: Int): Double {
        return sqrt(dx2(x, y).toDouble() + dy2(x, y).toDouble())
    }

    private fun getShortestPath(): List<Pair<Int, Int>>{
        val pathEnergyMatrix = Array(this.energyMatrix.size){Array(this.energyMatrix[0].size){ DijkstrasPath(null, null)}}
        //calculate paths
        for(i in pathEnergyMatrix.indices){
            for(j in pathEnergyMatrix[i].indices){
                //set first row to have it's own energy value, no adds.
                if(i == 0)
                    pathEnergyMatrix[i][j] = DijkstrasPath(null, this.energyMatrix[i][j])

                //Also calculate energy of adjacent squares
                //check if there are adjacent square
                if(i+1 in pathEnergyMatrix.indices) {
                    // down left
                    if(j-1 in  pathEnergyMatrix[i + 1].indices) {
                        val currentEnergy = pathEnergyMatrix[i + 1][j - 1].energy
                        val potentialEnergy = pathEnergyMatrix[i][j].energy!! + this.energyMatrix[i + 1][j - 1]

                        if(currentEnergy == null || currentEnergy > potentialEnergy)
                            pathEnergyMatrix[i + 1][j - 1] = DijkstrasPath(Pair(i, j), potentialEnergy)
                    }
                    //straight down
                    if(j in pathEnergyMatrix[i + 1].indices) {
                        val currentEnergy = pathEnergyMatrix[i + 1][j].energy
                        val potentialEnergy = pathEnergyMatrix[i][j].energy!! + this.energyMatrix[i + 1][j]

                        if(currentEnergy == null || currentEnergy > potentialEnergy)
                            pathEnergyMatrix[i + 1][j] = DijkstrasPath(Pair(i, j), potentialEnergy)
                    }
                    //down right
                    if(j+1 in pathEnergyMatrix[i + 1].indices) {
                        val currentEnergy = pathEnergyMatrix[i + 1][j + 1].energy
                        val potentialEnergy = pathEnergyMatrix[i][j].energy!! + this.energyMatrix[i + 1][j + 1]

                        if(currentEnergy == null || currentEnergy > potentialEnergy)
                            pathEnergyMatrix[i + 1][j + 1] = DijkstrasPath(Pair(i, j), potentialEnergy)
                    }
                }
            }
        }

        //return shortest path:
        val path = mutableListOf<Pair<Int, Int>>()
        val minimumPath = pathEnergyMatrix.last().minByOrNull { it.energy!! }!!
        var pathPair: Pair<Int, Int>? = Pair(pathEnergyMatrix.lastIndex, pathEnergyMatrix.last().indexOf(minimumPath))

        do {
            path.add(pathPair!!)
            pathPair = pathEnergyMatrix[pathPair.first][pathPair.second].previous
        } while (pathPair != null)

        return path
    }

    private fun createEnergyMatrix(width: Int, height: Int, orientation: String) {
        energyMatrix = Array(height) {Array(width) {0.0} }
        for (y in energyMatrix.indices) {
            for (x in energyMatrix[y].indices) {
                if (orientation == "horiz") {
                    energyMatrix[y][x] = pixelEnergy(y, x)
                } else {
                    energyMatrix[y][x] = pixelEnergy(x, y)
                }
            }
        }
    }

    private fun findHorizontalSeam() {

        resizeImage = BufferedImage(bufferedImage.width, bufferedImage.height - 1, BufferedImage.TYPE_INT_RGB)
        createEnergyMatrix(bufferedImage.height, bufferedImage.width, "horiz")

        val path = getShortestPath()

        for (x in 0 until bufferedImage.width) {
            var bias = 0
            for (y in 0 until bufferedImage.height) {
                if (path.contains(Pair(x, y))) {
                    bias = 1
                } else {
                    try {
                        resizeImage.setRGB(x, y - bias, bufferedImage.getRGB(x, y))
                    } catch (e: Exception) {
                        println(e.message)
                        return
                    }
                }
            }
        }
        bufferedImage = resizeImage
    }

    private fun findVerticalSeam() {

        resizeImage = BufferedImage(bufferedImage.width - 1, bufferedImage.height, BufferedImage.TYPE_INT_RGB)
        createEnergyMatrix(bufferedImage.width, bufferedImage.height, "vertical")

        val path = getShortestPath()

        for (y in 0 until bufferedImage.height) {
            var bias = 0
            for (x in 0 until bufferedImage.width) {
                if (path.contains(Pair(y, x))) {
                    bias = 1
                } else {
                    try {
                        resizeImage.setRGB(x - bias, y, bufferedImage.getRGB(x, y))
                    } catch (e: Exception) {
                        println(e.message)
                        return
                    }
                }
            }
        }
        bufferedImage = resizeImage
    }

    fun cutImage() {
        val newWidth = bufferedImage.width - width
        val newHeight = bufferedImage.height - height

        while (bufferedImage.height != newHeight ||
                bufferedImage.width != newWidth) {
            if (bufferedImage.height != newHeight) {
                findHorizontalSeam()
            }
            if (bufferedImage.width != newWidth) {
                findVerticalSeam()
            }

        }
        ImageIO.write(bufferedImage, "PNG", fileOut)
    }
}