import Foundation
import AVFoundation
import ImageIO
import CoreGraphics
import UniformTypeIdentifiers

// mp4gif <in.mp4> <out.gif> <fps> <width> [startSeconds] [endSeconds]
let a = CommandLine.arguments
guard a.count >= 5 else { fputs("usage: mp4gif in.mp4 out.gif fps width [start:end,start:end,...]\n", stderr); exit(2) }
let inURL = URL(fileURLWithPath: a[1]), outURL = URL(fileURLWithPath: a[2])
let fps = Double(a[3])!, width = CGFloat(Double(a[4])!)
// Ranges as "start:end,start:end,..." — a recording driven by a tool with seconds of
// latency between actions is mostly dead air, and the interesting windows stitch into
// one clip.
let rangeArg = a.count > 5 ? a[5] : ""
let asset = AVURLAsset(url: inURL)
let sem = DispatchSemaphore(value: 0)
var duration: Double = 0
Task { duration = try await asset.load(.duration).seconds; sem.signal() }
sem.wait()
var ranges: [(Double, Double)] = rangeArg.isEmpty ? [(0, duration)] : rangeArg
    .split(separator: ",")
    .map { pair -> (Double, Double) in
        let p = pair.split(separator: ":")
        return (Double(p[0])!, min(Double(p[1])!, duration))
    }

/// Re-draws into plain 8-bit sRGB. Video frames can carry a wide-gamut or 10-bit
/// colour space that the GIF encoder refuses outright.
func redraw(_ img: CGImage) -> CGImage? {
    guard let ctx = CGContext(
        data: nil, width: img.width, height: img.height,
        bitsPerComponent: 8, bytesPerRow: 0,
        space: CGColorSpace(name: CGColorSpace.sRGB)!,
        bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
    ) else { return nil }
    ctx.draw(img, in: CGRect(x: 0, y: 0, width: img.width, height: img.height))
    return ctx.makeImage()
}

let gen = AVAssetImageGenerator(asset: asset)
gen.appliesPreferredTrackTransform = true
gen.requestedTimeToleranceBefore = .zero
gen.requestedTimeToleranceAfter = .zero
gen.maximumSize = CGSize(width: width, height: width * 20)

// Collect first, create the destination second: CGImageDestinationFinalize fails if
// fewer images are added than the count the destination was created with, and a frame
// can always fail to generate.
var frames: [CGImage] = []
for (start, end) in ranges {
    var t = start
    while t < end {
        if let img = try? gen.copyCGImage(at: CMTime(seconds: t, preferredTimescale: 600), actualTime: nil),
           let flat = redraw(img) {
            frames.append(flat)
        }
        t += 1.0 / fps
    }
}
guard !frames.isEmpty else { fputs("no frames\n", stderr); exit(1) }

guard let dest = CGImageDestinationCreateWithURL(
    outURL as CFURL, UTType.gif.identifier as CFString, frames.count, nil) else { exit(1) }
CGImageDestinationSetProperties(dest, [kCGImagePropertyGIFDictionary: [kCGImagePropertyGIFLoopCount: 0]] as CFDictionary)
let frameProps = [kCGImagePropertyGIFDictionary: [kCGImagePropertyGIFDelayTime: 1.0 / fps]] as CFDictionary
for f in frames { CGImageDestinationAddImage(dest, f, frameProps) }
guard CGImageDestinationFinalize(dest) else { fputs("finalize failed\n", stderr); exit(1) }
let n = frames.count
print("\(n) frames -> \(outURL.path)")
