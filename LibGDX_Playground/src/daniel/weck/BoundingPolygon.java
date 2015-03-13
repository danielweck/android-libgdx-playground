package daniel.weck;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class BoundingPolygon {

	/**
	 * Find the convex hull of a point cloud using "Gift-wrap" algorithm - start
	 * with an external point, and walk around the outside edge by testing
	 * angles. Runs in O(N*S) time where S is number of sides of resulting
	 * polygon. Worst case: point cloud is all vertices of convex polygon ->
	 * O(N^2). There may be faster algorithms to do this, should you need one -
	 * this is just the simplest. You can get O(N log N) expected time if you
	 * try, I think, and O(N) if you restrict inputs to simple polygons. Returns
	 * null if number of vertices passed is less than 3. Results should be
	 * passed through convex decomposition afterwards to ensure that each shape
	 * has few enough points to be used in Box2d. May be buggy with colinear
	 * points on hull, but we check angle with an equality resolver that always
	 * picks the longer edge (this seems to be working, but it sometimes creates
	 * an extra edge along a line).
	 */
	public static Array<Vector2> createGiftWrapConvexHull(
			Array<Vector2> points) {
		assert (points.size > 2);

		int[] edgeList = new int[points.size];
		int numEdges = 0;
		float minY = Float.MAX_VALUE;
		int minYIndex = points.size;
		for (int i = 0; i < points.size; ++i) {
			Vector2 point = points.get(i);
			if (point.y < minY) {
				minY = point.y;
				minYIndex = i;
			}
		}
		int startIndex = minYIndex;
		int winIndex = -1;
		float dx = -1.0f;
		float dy = 0.0f;
		while (winIndex != minYIndex) {
			float newdx = 0.0f;
			float newdy = 0.0f;
			float maxDot = -2.0f;
			Vector2 point2 = points.get(startIndex);

			for (int i = 0; i < points.size; ++i) {
				if (i == startIndex)
					continue;
				Vector2 point1 = points.get(i);

				newdx = point1.x - point2.x;
				newdy = point1.y - point2.y;
				float nrm = (float) Math.sqrt(newdx * newdx + newdy * newdy);
				nrm = (nrm == 0.0f) ? 1.0f : nrm;
				newdx /= nrm;
				newdy /= nrm;
				float newDot = newdx * dx + newdy * dy;
				if (newDot > maxDot) {
					maxDot = newDot;
					winIndex = i;
				}
			}
			edgeList[numEdges] = winIndex;
			numEdges++;

			Vector2 point3 = points.get(winIndex);

			dx = point3.x - point2.x;
			dy = point3.y - point2.y;
			float nrm = (float) Math.sqrt(dx * dx + dy * dy);
			nrm = (nrm == 0.0f) ? 1.0f : nrm;
			dx /= nrm;
			dy /= nrm;
			startIndex = winIndex;
		}
		Array<Vector2> polygon = new Array<Vector2>(numEdges);

		for (int i = 0; i < numEdges; i++) {
			Vector2 point4 = points.get(edgeList[i]);
			polygon.add(point4.cpy());
		}

		if (polygon.size <= 3)
			return polygon;

		float tolerance = 2.0f / 180.0f * (float) Math.PI; // 2 degrees

		Array<Integer> toRemove = null;
		for (int i = 0; i < polygon.size; ++i) {
			int lower = (i == 0) ? (polygon.size - 1) : (i - 1);
			int middle = i;
			int upper = (i == polygon.size - 1) ? (0) : (i + 1);
			Vector2 pointMiddle = polygon.get(middle);
			Vector2 pointLower = polygon.get(lower);
			Vector2 pointUpper = polygon.get(upper);
			float dx0 = pointMiddle.x - pointLower.x;
			float dy0 = pointMiddle.y - pointLower.y;
			float dx1 = pointUpper.x - pointMiddle.x;
			float dy1 = pointUpper.y - pointMiddle.y;
			float norm0 = (float) Math.sqrt(dx0 * dx0 + dy0 * dy0);
			float norm1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
			if (!(norm0 > 0.0f && norm1 > 0.0f)
					&& (toRemove == null || (polygon.size - toRemove.size) > 3)) {
				// Merge identical points

				if (toRemove == null)
					toRemove = new Array<Integer>();
				toRemove.add(i);
			}
			dx0 /= norm0;
			dy0 /= norm0;
			dx1 /= norm1;
			dy1 /= norm1;
			float cross = dx0 * dy1 - dx1 * dy0;
			float dot = dx0 * dx1 + dy0 * dy1;
			if (Math.abs(cross) < tolerance && dot > 0
					&& (toRemove == null || (polygon.size - toRemove.size) > 3)) {
				if (toRemove == null)
					toRemove = new Array<Integer>();
				toRemove.add(i);
			}
		}
		if (toRemove == null || toRemove.size == 0)
			return polygon;

		for (int i = 0; i < toRemove.size; ++i) {
			int index = toRemove.get(i);
			polygon.removeIndex(index - i);
		}

		return polygon;
	}
}
