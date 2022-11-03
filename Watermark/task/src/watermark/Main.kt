package watermark

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() {

    println("Input the image filename:")
    val inputFile = File(readLine()  ?: "")
    val myImage: BufferedImage = checkImage(inputFile)


    println("Input the watermark image filename:")
    val watermarkFile = File(readLine() ?: "")
    val watermark: BufferedImage = checkWatermark(watermarkFile, myImage)

    var useTransparency = false
    var transparencyColor = listOf<Int>()

    if (watermark.transparency == 3) {
        println("Do you want to use the watermark's Alpha channel?")
        val useTransparencyAsnwer = readLine()!!.lowercase()
        if (useTransparencyAsnwer == "yes") useTransparency = true
    } else {
        println("Do you want to set a transparency color?")
        val useTransparencyAsnwer = readLine()!!.lowercase()
        if (useTransparencyAsnwer == "yes") {
            println("Input a transparency color ([Red] [Green] [Blue]):")
            try {
                transparencyColor = readLine()!!.split(" ").map { it.toInt() }
                transparencyColor.forEach { if (it < 0 || it > 255) throw NumberFormatException() }
                if (transparencyColor.size != 3) {
                    throw NumberFormatException()
                }

            } catch (e: NumberFormatException) {
                println("The transparency color input is invalid.")
                exitProcess(12)
            }
        }
    }


    println("Input the watermark transparency percentage (Integer 0-100):")
    val weight = try {
        val temp = readLine()!!.toInt()
        if (temp !in 0..100) {
            println("The transparency percentage is out of range.")
            exitProcess(8)
        }
        temp
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(9)
    }

    println("Choose the position method (single, grid):")
    var isGrid = false
    var offsets = listOf<Int>(0,0)
    val tileMode = readLine()!!.lowercase()
    if (tileMode == "single") {
        isGrid = false
        println("Input the watermark position ([x 0-${myImage.width - watermark.width}] [y 0-${myImage.height - watermark.height}]):")
        try {
            offsets = readLine()!!.split(" ").map { it.toInt() }
            if (offsets[0] < 0 || offsets[0] > myImage.width - watermark.width ||
                offsets[1] < 0 || offsets[1] > myImage.height - watermark.height) {
                println("The position input is out of range.")
                exitProcess(14)
            }
            if (offsets.size != 2) {
                throw NumberFormatException()
            }

        } catch (e: NumberFormatException) {
            println("The position input is invalid.")
            exitProcess(12)
        }
    }
    else if (tileMode == "grid") isGrid = true
    else {
        println("The position method input is invalid.")
        exitProcess(13)
    }

    println("Input the output image filename (jpg or png extension):")
    val outFileName =  readLine() ?: ""

    val extension = outFileName.takeLast(3).lowercase()
    if (extension != "jpg" && extension != "png") {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(10)
    }

    processImage(myImage, watermark, weight, useTransparency, transparencyColor, isGrid, offsets[0], offsets[1])
    ImageIO.write(myImage, extension, File(outFileName))
    println("The watermarked image $outFileName has been created.")
}

private fun checkWatermark(
    watermarkFile: File,
    myImage: BufferedImage
): BufferedImage {
    if (!watermarkFile.exists()) {
        println("The file $watermarkFile doesn't exist.")
        exitProcess(4)
    }

    val watermark: BufferedImage = ImageIO.read(watermarkFile)

    if (watermark.colorModel.numColorComponents != 3) {
        println("The number of watermark color components isn't 3.")
        exitProcess(5)
    }
    if (watermark.colorModel.pixelSize != 24 && watermark.colorModel.pixelSize != 32) {
        println("The watermark isn't 24 or 32-bit.")
        exitProcess(6)
    }

    if (myImage.width < watermark.width || myImage.height < watermark.height) {
        println("The watermark's dimensions are larger.")
        exitProcess(7)
    }
    return watermark
}

private fun checkImage(inputFile: File): BufferedImage {
    if (!inputFile.exists() || inputFile == null) {
        println("The file $inputFile doesn't exist.")
        exitProcess(1)
    }
    val myImage: BufferedImage = ImageIO.read(inputFile)

    if (myImage.colorModel.numColorComponents != 3) {
        println("The number of image color components isn't 3.")
        exitProcess(2)
    }
    if (myImage.colorModel.pixelSize != 24 && myImage.colorModel.pixelSize != 32) {
        println("The image isn't 24 or 32-bit.")
        exitProcess(3)
    }
    return myImage
}


private fun processImage(
    myImage: BufferedImage,
    watermark: BufferedImage,
    weight: Int,
    useAlphaChanel: Boolean,
    transparencyColor: List<Int>,
    grid : Boolean,
    singleX : Int,
    singleY : Int,
) {
    var xCount = (myImage.width / watermark.width) + 1
    var yCount = (myImage.height / watermark.height) + 1
    var xOffset = 0
    var yOffset = 0

    if (!grid) {
          xCount = 1
          yCount = 1
          xOffset = singleX
          yOffset = singleY
    }
    repeat (yCount) {
        repeat (xCount) {
            for (y in 0 until watermark.height) {
                if (y + yOffset >= myImage.height) break
                for (x in 0 until watermark.width) {

                    if (x + xOffset >= myImage.width) break

                    val i = Color(myImage.getRGB(x + xOffset, y + yOffset))
                    val w = Color(watermark.getRGB(x, y), true)

                    if (transparencyColor.isNotEmpty()) {
                        val transparencyColor = Color(transparencyColor[0], transparencyColor[1], transparencyColor[2])
                        if ((w.rgb == transparencyColor.rgb)) {
                            myImage.setRGB(x + xOffset, y + yOffset, i.rgb)
                        } else {
                            val color = Color(
                                (weight * w.red + (100 - weight) * i.red) / 100,
                                (weight * w.green + (100 - weight) * i.green) / 100,
                                (weight * w.blue + (100 - weight) * i.blue) / 100
                            )
                            myImage.setRGB(x + xOffset, y + yOffset, color.rgb)
                        }
                    } else if (w.alpha == 255 || !useAlphaChanel) {
                        val color = Color(
                            (weight * w.red + (100 - weight) * i.red) / 100,
                            (weight * w.green + (100 - weight) * i.green) / 100,
                            (weight * w.blue + (100 - weight) * i.blue) / 100
                        )
                        myImage.setRGB(x + xOffset, y + yOffset, color.rgb)
                    } else {
                        myImage.setRGB(x + xOffset, y + yOffset, i.rgb)
                    }
                }
            }
            xOffset += watermark.width
        }
        xOffset = 0
        yOffset += watermark.height
    }
}


