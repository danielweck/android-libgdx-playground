package daniel.weck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

//http://www.quasimondo.com
public class Blur {

	//
	// final class BlurStack {
	// int r = 0;
	// int g = 0;
	// int b = 0;
	// int a = 0;
	// BlurStack next = null;
	// }
	//
	// short[] mul_table = new short[] { 1, 57, 41, 21, 203, 34, 97, 73, 227,
	// 91,
	// 149, 62, 105, 45, 39, 137, 241, 107, 3, 173, 39, 71, 65, 238, 219,
	// 101, 187, 87, 81, 151, 141, 133, 249, 117, 221, 209, 197, 187, 177,
	// 169, 5, 153, 73, 139, 133, 127, 243, 233, 223, 107, 103, 99, 191,
	// 23, 177, 171, 165, 159, 77, 149, 9, 139, 135, 131, 253, 245, 119,
	// 231, 224, 109, 211, 103, 25, 195, 189, 23, 45, 175, 171, 83, 81,
	// 79, 155, 151, 147, 9, 141, 137, 67, 131, 129, 251, 123, 30, 235,
	// 115, 113, 221, 217, 53, 13, 51, 50, 49, 193, 189, 185, 91, 179,
	// 175, 43, 169, 83, 163, 5, 79, 155, 19, 75, 147, 145, 143, 35, 69,
	// 17, 67, 33, 65, 255, 251, 247, 243, 239, 59, 29, 229, 113, 111,
	// 219, 27, 213, 105, 207, 51, 201, 199, 49, 193, 191, 47, 93, 183,
	// 181, 179, 11, 87, 43, 85, 167, 165, 163, 161, 159, 157, 155, 77,
	// 19, 75, 37, 73, 145, 143, 141, 35, 138, 137, 135, 67, 33, 131, 129,
	// 255, 63, 250, 247, 61, 121, 239, 237, 117, 29, 229, 227, 225, 111,
	// 55, 109, 216, 213, 211, 209, 207, 205, 203, 201, 199, 197, 195,
	// 193, 48, 190, 47, 93, 185, 183, 181, 179, 178, 176, 175, 173, 171,
	// 85, 21, 167, 165, 41, 163, 161, 5, 79, 157, 78, 154, 153, 19, 75,
	// 149, 74, 147, 73, 144, 143, 71, 141, 140, 139, 137, 17, 135, 134,
	// 133, 66, 131, 65, 129, 1 };
	//
	// short[] shg_table = new short[] { 0, 9, 10, 10, 14, 12, 14, 14, 16, 15,
	// 16,
	// 15, 16, 15, 15, 17, 18, 17, 12, 18, 16, 17, 17, 19, 19, 18, 19, 18,
	// 18, 19, 19, 19, 20, 19, 20, 20, 20, 20, 20, 20, 15, 20, 19, 20, 20,
	// 20, 21, 21, 21, 20, 20, 20, 21, 18, 21, 21, 21, 21, 20, 21, 17, 21,
	// 21, 21, 22, 22, 21, 22, 22, 21, 22, 21, 19, 22, 22, 19, 20, 22, 22,
	// 21, 21, 21, 22, 22, 22, 18, 22, 22, 21, 22, 22, 23, 22, 20, 23, 22,
	// 22, 23, 23, 21, 19, 21, 21, 21, 23, 23, 23, 22, 23, 23, 21, 23, 22,
	// 23, 18, 22, 23, 20, 22, 23, 23, 23, 21, 22, 20, 22, 21, 22, 24, 24,
	// 24, 24, 24, 22, 21, 24, 23, 23, 24, 21, 24, 23, 24, 22, 24, 24, 22,
	// 24, 24, 22, 23, 24, 24, 24, 20, 23, 22, 23, 24, 24, 24, 24, 24, 24,
	// 24, 23, 21, 23, 22, 23, 24, 24, 24, 22, 24, 24, 24, 23, 22, 24, 24,
	// 25, 23, 25, 25, 23, 24, 25, 25, 24, 22, 25, 25, 25, 24, 23, 24, 25,
	// 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 23, 25, 23, 24, 25, 25,
	// 25, 25, 25, 25, 25, 25, 25, 24, 22, 25, 25, 23, 25, 25, 20, 24, 25,
	// 24, 25, 25, 22, 24, 25, 24, 25, 24, 25, 25, 24, 25, 25, 25, 25, 22,
	// 25, 25, 25, 24, 25, 24, 25, 18 };
	//
	// void blurImage(int radius, Pixmap pixmap, int w_, int h_, int rectx,
	// int recty, int rectw, int recth) {
	//
	// int width = pixmap.getWidth();
	// int height = pixmap.getHeight();
	//
	// int xoffset = 0;
	// int yoffset = 0;
	//
	// int x, y, i, p, yp, yi, yw, r_sum, g_sum, b_sum, a_sum, r_out_sum,
	// g_out_sum, b_out_sum, a_out_sum, r_in_sum, g_in_sum, b_in_sum, a_in_sum,
	// pr, pg, pb, pa, rbs;
	//
	// int div = radius + radius + 1;
	// int w4 = width << 2;
	// int widthMinus1 = width - 1;
	// int heightMinus1 = height - 1;
	// int radiusPlus1 = radius + 1;
	// int sumFactor = radiusPlus1 * (radiusPlus1 + 1) / 2;
	//
	// BlurStack stackStart = new BlurStack();
	// BlurStack stack = stackStart;
	// BlurStack stackEnd = null;
	// for (i = 1; i < div; i++) {
	// stack = stack.next = new BlurStack();
	// if (i == radiusPlus1)
	// stackEnd = stack;
	// }
	// stack.next = stackStart;
	// BlurStack stackIn = null;
	// BlurStack stackOut = null;
	//
	// yw = yi = 0;
	//
	// int mul_sum = mul_table[radius];
	// int shg_sum = shg_table[radius];
	//
	// for (y = 0; y < height; y++) {
	// r_in_sum = g_in_sum = b_in_sum = a_in_sum = r_sum = g_sum = b_sum = a_sum
	// = 0;
	//
	// int yy = (int) (yi / width);
	// int xx = (int) (yi % height);
	//
	// // 32-bit RGBA8888
	// int pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// int mask = pixel & 0xFFFFFFFF;
	// int rr = (mask >> 24) & 0xff;
	// int gg = (mask >> 16) & 0xff;
	// int bb = (mask >> 8) & 0xff;
	// int aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0 || bb > 255) {
	// // break ! (assert doesn't always kick-in with the Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// r_out_sum = radiusPlus1 * (pr = rr);
	// g_out_sum = radiusPlus1 * (pg = gg);
	// b_out_sum = radiusPlus1 * (pb = bb);
	// a_out_sum = radiusPlus1 * (pa = aa);
	//
	// r_sum += sumFactor * pr;
	// g_sum += sumFactor * pg;
	// b_sum += sumFactor * pb;
	// a_sum += sumFactor * pa;
	//
	// stack = stackStart;
	//
	// for (i = 0; i < radiusPlus1; i++) {
	// stack.r = pr;
	// stack.g = pg;
	// stack.b = pb;
	// stack.a = pa;
	// stack = stack.next;
	// }
	//
	// for (i = 1; i < radiusPlus1; i++) {
	// p = yi + ((widthMinus1 < i ? widthMinus1 : i) << 2);
	//
	// yy = (int) (p / width);
	// xx = (int) (p % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// rr = (mask >> 24) & 0xff;
	// gg = (mask >> 16) & 0xff;
	// bb = (mask >> 8) & 0xff;
	// aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// r_sum += (stack.r = (pr = rr)) * (rbs = radiusPlus1 - i);
	// g_sum += (stack.g = (pg = gg)) * rbs;
	// b_sum += (stack.b = (pb = bb)) * rbs;
	// a_sum += (stack.a = (pa = aa)) * rbs;
	//
	// r_in_sum += pr;
	// g_in_sum += pg;
	// b_in_sum += pb;
	// a_in_sum += pa;
	//
	// stack = stack.next;
	// }
	//
	// stackIn = stackStart;
	// stackOut = stackEnd;
	// for (x = 0; x < width; x++) {
	//
	// aa = (a_sum * mul_sum) >> shg_sum;
	// pa = aa;
	// if (pa != 0) {
	// pa = 255 / pa;
	//
	// rr = ((r_sum * mul_sum) >> shg_sum) * pa;
	// gg = ((g_sum * mul_sum) >> shg_sum) * pa;
	// bb = ((b_sum * mul_sum) >> shg_sum) * pa;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 1;
	// }
	//
	// pixmap.setColor(rr / 255f, gg / 255f, bb / 255f, aa / 255f);
	// } else {
	// pixmap.setColor(0, 0, 0, aa / 255f);
	// }
	// yy = (int) (yi / width);
	// xx = (int) (yi % height);
	// pixmap.drawPixel(xx + xoffset, yy + yoffset);
	//
	// r_sum -= r_out_sum;
	// g_sum -= g_out_sum;
	// b_sum -= b_out_sum;
	// a_sum -= a_out_sum;
	//
	// r_out_sum -= stackIn.r;
	// g_out_sum -= stackIn.g;
	// b_out_sum -= stackIn.b;
	// a_out_sum -= stackIn.a;
	//
	// p = (yw + ((p = x + radius + 1) < widthMinus1 ? p : widthMinus1)) << 2;
	//
	// yy = (int) (p / width);
	// xx = (int) (p % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// rr = (mask >> 24) & 0xff;
	// gg = (mask >> 16) & 0xff;
	// bb = (mask >> 8) & 0xff;
	// aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// r_in_sum += (stackIn.r = rr);
	// g_in_sum += (stackIn.g = gg);
	// b_in_sum += (stackIn.b = bb);
	// a_in_sum += (stackIn.a = aa);
	//
	// r_sum += r_in_sum;
	// g_sum += g_in_sum;
	// b_sum += b_in_sum;
	// a_sum += a_in_sum;
	//
	// stackIn = stackIn.next;
	//
	// r_out_sum += (pr = stackOut.r);
	// g_out_sum += (pg = stackOut.g);
	// b_out_sum += (pb = stackOut.b);
	// a_out_sum += (pa = stackOut.a);
	//
	// r_in_sum -= pr;
	// g_in_sum -= pg;
	// b_in_sum -= pb;
	// a_in_sum -= pa;
	//
	// stackOut = stackOut.next;
	//
	// yi += 4;
	// }
	// yw += width;
	// }
	//
	// for (x = 0; x < width; x++) {
	// g_in_sum = b_in_sum = a_in_sum = r_in_sum = g_sum = b_sum = a_sum = r_sum
	// = 0;
	//
	// yi = x << 2;
	//
	// int yy = (int) (yi / width);
	// int xx = (int) (yi % height);
	//
	// // 32-bit RGBA8888
	// int pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// int mask = pixel & 0xFFFFFFFF;
	// int rr = (mask >> 24) & 0xff;
	// int gg = (mask >> 16) & 0xff;
	// int bb = (mask >> 8) & 0xff;
	// int aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0 || bb > 255) {
	// // break ! (assert doesn't always kick-in with the Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// r_out_sum = radiusPlus1 * (pr = rr);
	// g_out_sum = radiusPlus1 * (pg = gg);
	// b_out_sum = radiusPlus1 * (pb = bb);
	// a_out_sum = radiusPlus1 * (pa = aa);
	//
	// r_sum += sumFactor * pr;
	// g_sum += sumFactor * pg;
	// b_sum += sumFactor * pb;
	// a_sum += sumFactor * pa;
	//
	// stack = stackStart;
	//
	// for (i = 0; i < radiusPlus1; i++) {
	// stack.r = pr;
	// stack.g = pg;
	// stack.b = pb;
	// stack.a = pa;
	// stack = stack.next;
	// }
	//
	// yp = width;
	//
	// for (i = 1; i <= radius; i++) {
	// yi = (yp + x) << 2;
	//
	// yy = (int) (yi / width);
	// xx = (int) (yi % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// rr = (mask >> 24) & 0xff;
	// gg = (mask >> 16) & 0xff;
	// bb = (mask >> 8) & 0xff;
	// aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	// r_sum += (stack.r = (pr = rr)) * (rbs = radiusPlus1 - i);
	// g_sum += (stack.g = (pg = gg)) * rbs;
	// b_sum += (stack.b = (pb = bb)) * rbs;
	// a_sum += (stack.a = (pa = aa)) * rbs;
	//
	// r_in_sum += pr;
	// g_in_sum += pg;
	// b_in_sum += pb;
	// a_in_sum += pa;
	//
	// stack = stack.next;
	//
	// if (i < heightMinus1) {
	// yp += width;
	// }
	// }
	//
	// yi = x;
	// stackIn = stackStart;
	// stackOut = stackEnd;
	// for (y = 0; y < height; y++) {
	// p = yi << 2;
	//
	// aa = (a_sum * mul_sum) >> shg_sum;
	// pa = aa;
	// if (pa > 0) {
	// pa = 255 / pa;
	// rr = ((r_sum * mul_sum) >> shg_sum) * pa;
	// gg = ((g_sum * mul_sum) >> shg_sum) * pa;
	// bb = ((b_sum * mul_sum) >> shg_sum) * pa;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 1;
	// }
	// pixmap.setColor(rr / 255f, gg / 255f, bb / 255f, aa / 255f);
	// } else {
	// pixmap.setColor(0, 0, 0, aa / 255f);
	// }
	//
	// yy = (int) (p / width);
	// xx = (int) (p % height);
	// pixmap.drawPixel(xx + xoffset, yy + yoffset);
	//
	// r_sum -= r_out_sum;
	// g_sum -= g_out_sum;
	// b_sum -= b_out_sum;
	// a_sum -= a_out_sum;
	//
	// r_out_sum -= stackIn.r;
	// g_out_sum -= stackIn.g;
	// b_out_sum -= stackIn.b;
	// a_out_sum -= stackIn.a;
	//
	// p = (x + (((p = y + radiusPlus1) < heightMinus1 ? p
	// : heightMinus1) * width)) << 2;
	//
	// yy = (int) (p / width);
	// xx = (int) (p % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// rr = (mask >> 24) & 0xff;
	// gg = (mask >> 16) & 0xff;
	// bb = (mask >> 8) & 0xff;
	// aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	// r_sum += (r_in_sum += (stackIn.r = rr));
	// g_sum += (g_in_sum += (stackIn.g = gg));
	// b_sum += (b_in_sum += (stackIn.b = bb));
	// a_sum += (a_in_sum += (stackIn.a = gg));
	//
	// stackIn = stackIn.next;
	//
	// r_out_sum += (pr = stackOut.r);
	// g_out_sum += (pg = stackOut.g);
	// b_out_sum += (pb = stackOut.b);
	// a_out_sum += (pa = stackOut.a);
	//
	// r_in_sum -= pr;
	// g_in_sum -= pg;
	// b_in_sum -= pb;
	// a_in_sum -= pa;
	//
	// stackOut = stackOut.next;
	//
	// yi += width;
	// }
	// }
	// }

	//
	// short[] mul_table = new short[] { 512, 512, 456, 512, 328, 456, 335, 512,
	// 405, 328, 271, 456, 388, 335, 292, 512, 454, 405, 364, 328, 298,
	// 271, 496, 456, 420, 388, 360, 335, 312, 292, 273, 512, 482, 454,
	// 428, 405, 383, 364, 345, 328, 312, 298, 284, 271, 259, 496, 475,
	// 456, 437, 420, 404, 388, 374, 360, 347, 335, 323, 312, 302, 292,
	// 282, 273, 265, 512, 497, 482, 468, 454, 441, 428, 417, 405, 394,
	// 383, 373, 364, 354, 345, 337, 328, 320, 312, 305, 298, 291, 284,
	// 278, 271, 265, 259, 507, 496, 485, 475, 465, 456, 446, 437, 428,
	// 420, 412, 404, 396, 388, 381, 374, 367, 360, 354, 347, 341, 335,
	// 329, 323, 318, 312, 307, 302, 297, 292, 287, 282, 278, 273, 269,
	// 265, 261, 512, 505, 497, 489, 482, 475, 468, 461, 454, 447, 441,
	// 435, 428, 422, 417, 411, 405, 399, 394, 389, 383, 378, 373, 368,
	// 364, 359, 354, 350, 345, 341, 337, 332, 328, 324, 320, 316, 312,
	// 309, 305, 301, 298, 294, 291, 287, 284, 281, 278, 274, 271, 268,
	// 265, 262, 259, 257, 507, 501, 496, 491, 485, 480, 475, 470, 465,
	// 460, 456, 451, 446, 442, 437, 433, 428, 424, 420, 416, 412, 408,
	// 404, 400, 396, 392, 388, 385, 381, 377, 374, 370, 367, 363, 360,
	// 357, 354, 350, 347, 344, 341, 338, 335, 332, 329, 326, 323, 320,
	// 318, 315, 312, 310, 307, 304, 302, 299, 297, 294, 292, 289, 287,
	// 285, 282, 280, 278, 275, 273, 271, 269, 267, 265, 263, 261, 259 };
	//
	// short[] shg_table = new short[] { 9, 11, 12, 13, 13, 14, 14, 15, 15, 15,
	// 15, 16, 16, 16, 16, 17, 17, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18,
	// 18, 18, 18, 18, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19,
	// 19, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
	// 20, 20, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21,
	// 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 22, 22, 22, 22, 22,
	// 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
	// 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 23, 23,
	// 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
	// 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
	// 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
	// 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
	// 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
	// 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
	// 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
	// 24, 24, 24, 24, 24, 24, 24 };
	//
	// void blurImage(int radius, Pixmap pixmap, int w_, int h_, int rectx,
	// int recty, int rectw, int recth) {
	//
	// int width = pixmap.getWidth();
	// int height = pixmap.getHeight();
	//
	// int xoffset = 0;
	// int yoffset = 0;
	//
	// int iterations = 2;
	//
	// int rsum, gsum, bsum, asum, x, y, i, p, p1, p2, yp, yi, yw, idx, pa;
	// int wm = width - 1;
	// int hm = height - 1;
	// int wh = width * height;
	// int rad1 = radius + 1;
	//
	// int mul_sum = mul_table[radius];
	// int shg_sum = shg_table[radius];
	//
	// int r[] = new int[wh];
	// int g[] = new int[wh];
	// int b[] = new int[wh];
	// int a[] = new int[wh];
	//
	// int vmin[] = new int[(int) Math.max(width, height)];
	// int vmax[] = new int[(int) Math.max(width, height)];
	//
	// while (iterations-- > 0) {
	// yw = yi = 0;
	//
	// for (y = 0; y < height; y++) {
	//
	// int yy = (int) (yw / width);
	// int xx = (int) (yw % height);
	//
	// // 32-bit RGBA8888
	// int pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// int mask = pixel & 0xFFFFFFFF;
	// int rr = (mask >> 24) & 0xff;
	// int gg = (mask >> 16) & 0xff;
	// int bb = (mask >> 8) & 0xff;
	// int aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// rsum = rr * rad1;
	// gsum = gg * rad1;
	// bsum = bb * rad1;
	// asum = aa * rad1;
	//
	// for (i = 1; i <= radius; i++) {
	// p = yw + (((i > wm ? wm : i)) << 2);
	//
	// // p++;
	// yy = (int) (p / width);
	// xx = (int) (p % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// rr = (mask >> 24) & 0xff;
	// gg = (mask >> 16) & 0xff;
	// bb = (mask >> 8) & 0xff;
	// aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// rsum += rr;
	// gsum += gg;
	// bsum += bb;
	// asum += aa;
	// }
	//
	// for (x = 0; x < width; x++) {
	// r[yi] = rsum;
	// g[yi] = gsum;
	// b[yi] = bsum;
	// a[yi] = asum;
	//
	// if (y == 0) {
	// vmin[x] = ((p = x + rad1) < wm ? p : wm) << 2;
	// vmax[x] = ((p = x - radius) > 0 ? p << 2 : 0);
	// }
	//
	// p1 = yw + vmin[x];
	// p2 = yw + vmax[x];
	//
	// yy = (int) (p1 / width);
	// xx = (int) (p1 % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// rr = (mask >> 24) & 0xff;
	// gg = (mask >> 16) & 0xff;
	// bb = (mask >> 8) & 0xff;
	// aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// yy = (int) (p2 / width);
	// xx = (int) (p2 % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// int rrr = (mask >> 24) & 0xff;
	// int ggg = (mask >> 16) & 0xff;
	// int bbb = (mask >> 8) & 0xff;
	// int aaa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// rsum += rr - rrr;
	// gsum += gg - ggg;
	// bsum += bb - bbb;
	// asum += aa - aaa;
	//
	// yi++;
	// }
	// yw += (width << 2);
	// }
	//
	// for (x = 0; x < width; x++) {
	// yp = x;
	// rsum = r[yp] * rad1;
	// gsum = g[yp] * rad1;
	// bsum = b[yp] * rad1;
	// asum = a[yp] * rad1;
	//
	// for (i = 1; i <= radius; i++) {
	// yp += (i > hm ? 0 : width);
	// rsum += r[yp];
	// gsum += g[yp];
	// bsum += b[yp];
	// asum += a[yp];
	// }
	//
	// yi = x << 2;
	// for (y = 0; y < height; y++) {
	//
	// int yy = (int) (yi / width);
	// int xx = (int) (yi % height);
	//
	//
	// pa = (asum * mul_sum) >>> shg_sum;
	// int aa = pa;
	// if (pa > 0) {
	// pa = 255 / pa;
	// int rr = ((rsum * mul_sum) >>> shg_sum) * pa;
	// int gg = ((gsum * mul_sum) >>> shg_sum) * pa;
	// int bb = ((bsum * mul_sum) >>> shg_sum) * pa;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// pixmap.setColor(rr / 255f, gg / 255f, bb / 255f,
	// aa / 255f);
	//
	// } else {
	// pixmap.setColor(0, 0, 0, aa);
	// }
	//
	// pixmap.drawPixel(xx + xoffset, yy + yoffset);
	//
	// if (x == 0) {
	// vmin[y] = ((p = y + rad1) < hm ? p : hm) * width;
	// vmax[y] = ((p = y - radius) > 0 ? p * width : 0);
	// }
	//
	// p1 = x + vmin[y];
	// p2 = x + vmax[y];
	//
	// rsum += r[p1] - r[p2];
	// gsum += g[p1] - g[p2];
	// bsum += b[p1] - b[p2];
	// asum += a[p1] - a[p2];
	//
	// yi += width << 2;
	// }
	// }
	// }
	// }

	// int[] mul_table = new int[] { 1, 171, 205, 293, 57, 373, 79, 137, 241,
	// 27, 391, 357, 41, 19, 283, 265, 497, 469, 443, 421, 25, 191, 365,
	// 349, 335, 161, 155, 149, 9, 278, 269, 261, 505, 245, 475, 231, 449,
	// 437, 213, 415, 405, 395, 193, 377, 369, 361, 353, 345, 169, 331,
	// 325, 319, 313, 307, 301, 37, 145, 285, 281, 69, 271, 267, 263, 259,
	// 509, 501, 493, 243, 479, 118, 465, 459, 113, 446, 55, 435, 429,
	// 423, 209, 413, 51, 403, 199, 393, 97, 3, 379, 375, 371, 367, 363,
	// 359, 355, 351, 347, 43, 85, 337, 333, 165, 327, 323, 5, 317, 157,
	// 311, 77, 305, 303, 75, 297, 294, 73, 289, 287, 71, 141, 279, 277,
	// 275, 68, 135, 67, 133, 33, 262, 260, 129, 511, 507, 503, 499, 495,
	// 491, 61, 121, 481, 477, 237, 235, 467, 232, 115, 457, 227, 451, 7,
	// 445, 221, 439, 218, 433, 215, 427, 425, 211, 419, 417, 207, 411,
	// 409, 203, 202, 401, 399, 396, 197, 49, 389, 387, 385, 383, 95, 189,
	// 47, 187, 93, 185, 23, 183, 91, 181, 45, 179, 89, 177, 11, 175, 87,
	// 173, 345, 343, 341, 339, 337, 21, 167, 83, 331, 329, 327, 163, 81,
	// 323, 321, 319, 159, 79, 315, 313, 39, 155, 309, 307, 153, 305, 303,
	// 151, 75, 299, 149, 37, 295, 147, 73, 291, 145, 289, 287, 143, 285,
	// 71, 141, 281, 35, 279, 139, 69, 275, 137, 273, 17, 271, 135, 269,
	// 267, 133, 265, 33, 263, 131, 261, 130, 259, 129, 257, 1 };
	//
	// int[] shg_table = new int[] { 0, 9, 10, 11, 9, 12, 10, 11, 12, 9, 13,
	// 13, 10, 9, 13, 13, 14, 14, 14, 14, 10, 13, 14, 14, 14, 13, 13, 13,
	// 9, 14, 14, 14, 15, 14, 15, 14, 15, 15, 14, 15, 15, 15, 14, 15, 15,
	// 15, 15, 15, 14, 15, 15, 15, 15, 15, 15, 12, 14, 15, 15, 13, 15, 15,
	// 15, 15, 16, 16, 16, 15, 16, 14, 16, 16, 14, 16, 13, 16, 16, 16, 15,
	// 16, 13, 16, 15, 16, 14, 9, 16, 16, 16, 16, 16, 16, 16, 16, 16, 13,
	// 14, 16, 16, 15, 16, 16, 10, 16, 15, 16, 14, 16, 16, 14, 16, 16, 14,
	// 16, 16, 14, 15, 16, 16, 16, 14, 15, 14, 15, 13, 16, 16, 15, 17, 17,
	// 17, 17, 17, 17, 14, 15, 17, 17, 16, 16, 17, 16, 15, 17, 16, 17, 11,
	// 17, 16, 17, 16, 17, 16, 17, 17, 16, 17, 17, 16, 17, 17, 16, 16, 17,
	// 17, 17, 16, 14, 17, 17, 17, 17, 15, 16, 14, 16, 15, 16, 13, 16, 15,
	// 16, 14, 16, 15, 16, 12, 16, 15, 16, 17, 17, 17, 17, 17, 13, 16, 15,
	// 17, 17, 17, 16, 15, 17, 17, 17, 16, 15, 17, 17, 14, 16, 17, 17, 16,
	// 17, 17, 16, 15, 17, 16, 14, 17, 16, 15, 17, 16, 17, 17, 16, 17, 15,
	// 16, 17, 14, 17, 16, 15, 17, 16, 17, 13, 17, 16, 17, 17, 16, 17, 14,
	// 17, 16, 17, 16, 17, 16, 17, 9 };
	//
	// final class BlurStack {
	// int r = 0;
	// int g = 0;
	// int b = 0;
	// int a = 0;
	// BlurStack next = null;
	// }
	//
	// void blurImage(int radius, Pixmap pixmap, int w_, int h_, int rectx,
	// int recty, int rectw, int recth) {
	//
	// int width = pixmap.getWidth();
	// int height = pixmap.getHeight();
	//
	// int xoffset = 0;
	// int yoffset = 0;
	//
	// int iterations = 2;
	//
	// int x, y, i, p, yp, yi, yw, r_sum, g_sum, b_sum, a_sum, r_out_sum,
	// g_out_sum, b_out_sum, a_out_sum, r_in_sum, g_in_sum, b_in_sum, a_in_sum,
	// pr, pg, pb, pa, rbs;
	//
	// int div = radius + radius + 1;
	// int w4 = width << 2;
	// int widthMinus1 = width - 1;
	// int heightMinus1 = height - 1;
	// int radiusPlus1 = radius + 1;
	//
	// BlurStack stackStart = new BlurStack();
	//
	// BlurStack stackEnd = null;
	// BlurStack stack = stackStart;
	// for (i = 1; i < div; i++) {
	// stack = stack.next = new BlurStack();
	// if (i == radiusPlus1)
	// stackEnd = stack;
	// }
	// stack.next = stackStart;
	// BlurStack stackIn = null;
	//
	// int mul_sum = mul_table[radius];
	// int shg_sum = shg_table[radius];
	// while (iterations-- > 0) {
	// yw = yi = 0;
	// for (y = height; --y > -1;) {
	//
	// int yy = (int) (yi / width);
	// int xx = (int) (yi % height);
	//
	// // 32-bit RGBA8888
	// int pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// int mask = pixel & 0xFFFFFFFF;
	// int rr = (mask >> 24) & 0xff;
	// int gg = (mask >> 16) & 0xff;
	// int bb = (mask >> 8) & 0xff;
	// int aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// r_sum = radiusPlus1 * (pr = rr);
	// g_sum = radiusPlus1 * (pg = gg);
	// b_sum = radiusPlus1 * (pb = bb);
	// a_sum = radiusPlus1 * (pa = aa);
	//
	// stack = stackStart;
	//
	// for (i = radiusPlus1; --i > -1;) {
	// stack.r = pr;
	// stack.g = pg;
	// stack.b = pb;
	// stack.a = pa;
	// stack = stack.next;
	// }
	//
	// for (i = 1; i < radiusPlus1; i++) {
	// p = yi + ((widthMinus1 < i ? widthMinus1 : i) << 2);
	//
	// yy = (int) (p / width);
	// xx = (int) (p % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// rr = (mask >> 24) & 0xff;
	// gg = (mask >> 16) & 0xff;
	// bb = (mask >> 8) & 0xff;
	// aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// r_sum += (stack.r = rr);
	// g_sum += (stack.g = gg);
	// b_sum += (stack.b = bb);
	// a_sum += (stack.a = aa);
	//
	// stack = stack.next;
	// }
	//
	// stackIn = stackStart;
	// for (x = 0; x < width; x++) {
	// rr = (r_sum * mul_sum) >>> shg_sum;
	// gg = (g_sum * mul_sum) >>> shg_sum;
	// bb = (b_sum * mul_sum) >>> shg_sum;
	// aa = (a_sum * mul_sum) >>> shg_sum;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// pixmap.setColor(rr / 255f, gg / 255f, bb / 255f, 1f);
	//
	// yy = (int) (yi / width);
	// xx = (int) (yi % height);
	// yi++;
	// pixmap.drawPixel(xx + xoffset, yy + yoffset);
	//
	// p = (yw + ((p = x + radius + 1) < widthMinus1 ? p
	// : widthMinus1)) << 2;
	//
	// yy = (int) (p / width);
	// xx = (int) (p % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// rr = (mask >> 24) & 0xff;
	// gg = (mask >> 16) & 0xff;
	// bb = (mask >> 8) & 0xff;
	// aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// r_sum -= stackIn.r - (stackIn.r = rr);
	// g_sum -= stackIn.g - (stackIn.g = gg);
	// b_sum -= stackIn.b - (stackIn.b = bb);
	// a_sum -= stackIn.a - (stackIn.a = aa);
	//
	// stackIn = stackIn.next;
	//
	// }
	// yw += width;
	// }
	//
	// for (x = 0; x < width; x++) {
	// yi = x << 2;
	//
	// int yy = (int) (yi / width);
	// int xx = (int) (yi % height);
	//
	// // 32-bit RGBA8888
	// int pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// int mask = pixel & 0xFFFFFFFF;
	// int rr = (mask >> 24) & 0xff;
	// int gg = (mask >> 16) & 0xff;
	// int bb = (mask >> 8) & 0xff;
	// int aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// r_sum = radiusPlus1 * (pr = rr);
	// g_sum = radiusPlus1 * (pg = gg);
	// b_sum = radiusPlus1 * (pb = bb);
	// a_sum = radiusPlus1 * (pa = aa);
	//
	// stack = stackStart;
	//
	// for (i = 0; i < radiusPlus1; i++) {
	// stack.r = pr;
	// stack.g = pg;
	// stack.b = pb;
	// stack.a = pa;
	// stack = stack.next;
	// }
	//
	// yp = width;
	//
	// for (i = 1; i <= radius; i++) {
	// yi = (yp + x) << 2;
	//
	// yy = (int) (yi / width);
	// xx = (int) (yi % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// rr = (mask >> 24) & 0xff;
	// gg = (mask >> 16) & 0xff;
	// bb = (mask >> 8) & 0xff;
	// aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// r_sum += (stack.r = rr);
	// g_sum += (stack.g = gg);
	// b_sum += (stack.b = bb);
	// a_sum += (stack.a = aa);
	//
	// stack = stack.next;
	//
	// if (i < heightMinus1) {
	// yp += width;
	// }
	// }
	//
	// yi = x;
	// stackIn = stackStart;
	// for (y = 0; y < height; y++) {
	// p = yi << 2;
	//
	// pa = (a_sum * mul_sum) >>> shg_sum;
	// aa = pa;
	// if (pa > 0) {
	// pa = 255 / pa;
	//
	// rr = ((r_sum * mul_sum) >>> shg_sum) * pa;
	// gg = ((g_sum * mul_sum) >>> shg_sum) * pa;
	// bb = ((b_sum * mul_sum) >>> shg_sum) * pa;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// pixmap.setColor(rr / 255f, gg / 255f, bb / 255f,
	// aa / 255f);
	// } else {
	//
	// pixmap.setColor(0, 0, 0, aa / 255f);
	// }
	// yy = (int) (p / width);
	// xx = (int) (p % height);
	//
	// pixmap.drawPixel(xx + xoffset, yy + yoffset);
	//
	// p = (x + (((p = y + radiusPlus1) < heightMinus1 ? p
	// : heightMinus1) * width)) << 2;
	//
	// yy = (int) (p / width);
	// xx = (int) (p % height);
	//
	// // 32-bit RGBA8888
	// pixel = pixmap.getPixel(xx + xoffset, yy + yoffset);
	//
	// mask = pixel & 0xFFFFFFFF;
	// rr = (mask >> 24) & 0xff;
	// gg = (mask >> 16) & 0xff;
	// bb = (mask >> 8) & 0xff;
	// aa = (mask) & 0xff;
	//
	// if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
	// || bb > 255) {
	// // break ! (assert doesn't always kick-in with the
	// // Eclipse
	// // debugger...)
	// int divide_by_zero = 0 / 0;
	// }
	//
	// r_sum -= stackIn.r - (stackIn.r = rr);
	// g_sum -= stackIn.g - (stackIn.g = gg);
	// b_sum -= stackIn.b - (stackIn.b = bb);
	// a_sum -= stackIn.a - (stackIn.a = aa);
	//
	// stackIn = stackIn.next;
	//
	// yi += width;
	// }
	// }
	// }
	// }

	public static void blurImage(int radius, Pixmap pixmap, int width, int height, int rectx,
			int recty, int rectw, int recth) {

		// GL10 gl = Gdx.graphics.getGL10();
		// gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		// Pixmap pixmap = new Pixmap(pixmapSource.getWidth(),
		// pixmapSource.getHeight(), pixmapSource.getFormat());
		// ByteBuffer destination = pixmap.getPixels();
		// BufferUtils.copy(source, destination, source.capacity());
		//
		// ByteBuffer pixels = pixmap.getPixels();
		// pixels.clear();
		//
		// while (pixels.hasRemaining()) {
		// //byte b = source.get();
		// pixels.put((byte)0);
		// }

		// // long mask1 = (long) color & 0xFFFFFFFFL;
		// // float r = (mask1 & 0xFF000000) / 255.0f;
		// // float g = (mask1 & 0x00FF0000) / 255.0f;
		// // float b = (mask1 & 0x0000FF00) / 255.0f;
		// // float a = (mask1 & 0x000000FF);

		// pixmap.setColor(1,0,0,1);
		// pixmap.fillCircle(w / 2, h / 2, Math.min(w / 4, h / 4));
		//

		// public static int rgba8888(float r, float g, float b, float
		// a) {
		// return ((int)(r*255) << 24) | ((int)(g*255) << 16) |
		// ((int)(b*255) << 8) | (int)(a*255);
		// }

		// RGBA ColourValue::getAsRGBA(void) const
		//
		// {
		// uint8 val8;
		// uint32 val32 = 0;
		//
		// // Convert to 32bit pattern
		// // (RGBA = 8888)
		//
		// // Red
		// val8 = static_cast<uint8>(r * 255);
		// val32 = val8 << 24;
		//
		// // Green
		// val8 = static_cast<uint8>(g * 255);
		// val32 += val8 << 16;
		//
		// // Blue
		// val8 = static_cast<uint8>(b * 255);
		// val32 += val8 << 8;
		//
		// // Alpha
		// val8 = static_cast<uint8>(a * 255);
		// val32 += val8;
		//
		// return val32;
		// }

		// void ColourValue::setAsRGBA(const RGBA val)
		// {
		// uint32 val32 = val;
		//
		// // Convert from 32bit pattern
		// // (RGBA = 8888)
		//
		// // Red
		// r = static_cast<uint8>(val32 >> 24) / 255.0f;
		//
		// // Green
		// g = static_cast<uint8>(val32 >> 16) / 255.0f;
		//
		// // Blue
		// b = static_cast<uint8>(val32 >> 8) / 255.0f;
		//
		// // Alpha
		// a = static_cast<uint8>(val32) / 255.0f;
		// }

		// int iA = (value >> 24) & 0xff;
		// int iR = (value >> 16) & 0xff;
		// int iG = (value >> 8) & 0xff;
		// int iB = (value) & 0xff;

		// p = Color.toIntBits(255, 0, 0, 0);

		int w = pixmap.getWidth();
		int h = pixmap.getHeight();

		int xoffset = 0;
		int yoffset = 0;

		// Gdx.app.log(APP_NAME, "Bluring Pixmap: " + w + "x" + h + " / " +
		// width+ "x" + height + " / " + rectx + "," + recty + "," + rectw+ ","
		// + recth);

		if (width > w) {
			// Gdx.app.log(APP_NAME, "Adjust (width > w): " + width + ">" + w);
			width = w;
		}
		if (height > h) {
			// Gdx.app.log(APP_NAME, "Adjust (height > h): " + height + ">" +
			// h);
			height = h;
		}

		w = width;
		h = height;

		if (rectx > width) {
			// Gdx.app.log(APP_NAME, "Adjust (rectx > width): " + rectx + ">" +
			// width);
			rectx = 0;
		}
		if (recty > height) {
			// Gdx.app.log(APP_NAME, "Adjust (recty > height): " + recty + ">" +
			// height);
			recty = 0;
		}

		if (rectx + rectw > width) {
			// Gdx.app.log(APP_NAME, "Adjust (rectx + rectw > width): " + rectx
			// + "+" + rectw + ">" + width);
			rectw = width - rectx;
		}
		if (recty + recth > height) {
			// Gdx.app.log(APP_NAME, "Adjust (recty + recth > height): " + recty
			// + "+" + recth + ">" + height);
			recth = height - recty;
		}

		xoffset = rectx;
		yoffset = recty;

		w = rectw;
		h = recth;

		pixmap.setColor(1, 0, 0, 1);
		pixmap.drawRectangle(xoffset, yoffset, w, h);

		// Gdx.app.log(APP_NAME, "Bluring Pixmap (adjusted): " + xoffset + "," +
		// yoffset + "/" + w + "x" + h);

		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;
		int div = radius + radius + 1;

		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[(int) Math.max(w, h)];

		int divsum = (div + 1) >> 1;
		divsum *= divsum;
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		}

		yw = yi = 0;

		int[][] stack = new int[div][3];
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;

		for (y = 0; y < h; y++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			for (i = -radius; i <= radius; i++) {

				int index = yi + Math.min(wm, (int) Math.max(i, 0));

				// p = pixels.getInt(index);

				int yy = (int) (index / w);
				int xx = (int) (index % h);

				// 32-bit RGBA8888
				p = pixmap.getPixel(xx + xoffset, yy + yoffset);

				int mask = p & 0xFFFFFFFF;
				int rr = (mask >> 24) & 0xff;
				int gg = (mask >> 16) & 0xff;
				int bb = (mask >> 8) & 0xff;
				int aa = (mask) & 0xff;

				if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
						|| bb > 255) {
					// break ! (assert doesn't always kick-in with the Eclipse
					// debugger...)
					int divide_by_zero = 0 / 0;
				}

				sir = stack[i + radius];

				sir[0] = rr;
				sir[1] = gg;
				sir[2] = bb;

				rbs = r1 - (int) Math.abs(i);
				rsum += sir[0] * rbs;
				gsum += sir[1] * rbs;
				bsum += sir[2] * rbs;
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}
			}
			stackpointer = radius;

			for (x = 0; x < w; x++) {

				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				int index = yw + vmin[x];

				// p = pixels.getInt(index);

				int yy = (int) (index / w);
				int xx = (int) (index % h);

				// 32-bit RGBA8888
				p = pixmap.getPixel(xx + xoffset, yy + yoffset);

				int mask = p & 0xFFFFFFFF;
				int rr = (mask >> 24) & 0xff;
				int gg = (mask >> 16) & 0xff;
				int bb = (mask >> 8) & 0xff;
				int aa = (mask) & 0xff;

				if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
						|| bb > 255) {
					// break ! (assert doesn't always kick-in with the Eclipse
					// debugger...)
					int divide_by_zero = 0 / 0;
				}

				sir[0] = rr;
				sir[1] = gg;
				sir[2] = bb;

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[(stackpointer) % div];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi++;
			}
			yw += w;
		}
		for (x = 0; x < w; x++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = (int) Math.max(0, yp) + x;

				sir = stack[i + radius];

				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];

				rbs = r1 - (int) Math.abs(i);

				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;

				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}

				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {

				// int value = 0x000000ff | (dv[rsum] << 24) | (dv[gsum] << 16)
				// | dv[bsum] << 8;
				// pixels.putInt(yi, value);
				// int value = Color.rgba8888(dv[rsum]/ 255f, dv[gsum]/ 255f,
				// dv[bsum]/ 255f, 1);
				// int mask = value & 0xFFFFFFFF;
				// int rr = (mask >> 24) & 0xff;
				// int gg = (mask >> 16) & 0xff;
				// int bb = (mask >> 8) & 0xff;
				// int aa = (mask) & 0xff;

				int rr = dv[rsum];
				int gg = dv[gsum];
				int bb = dv[bsum];

				if (rr < 0 || rr > 255 || gg < 0 || gg > 255 || bb < 0
						|| bb > 255) {
					// break ! (assert doesn't always kick-in with the Eclipse
					// debugger...)
					int divide_by_zero = 0 / 0;
				}

				pixmap.setColor(rr / 255f, gg / 255f, bb / 255f, 1f);

				int yy = (int) (yi / w);
				int xx = (int) (yi % h);
				pixmap.drawPixel(xx + xoffset, yy + yoffset);

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (x == 0) {
					vmin[y] = Math.min(y + r1, hm) * w;
				}
				p = x + vmin[y];

				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[stackpointer];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi += w;
			}
		}

		// Gdx.app.log(APP_NAME, "Blured Pixmap with radius: " + radius);
	}
}
