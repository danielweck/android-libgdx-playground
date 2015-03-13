package daniel.weck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

//Taken from CuttingTools.cs (FarseerPhysics.Common.PolygonManipulation.CuttingTools)
//at http://farseerphysics.codeplex.com

public final class BoxCutter {

	// Cutting a shape into two is based on the work of Daid and his prototype
	// BoxCutter: http://www.box2d.org/forum/viewtopic.php?f=3&t=1473

	// / <summary>
	// / Split a fixture into 2 vertice collections using the given entry and
	// exit-point.
	// / </summary>
	// / <param name="fixture">The Fixture to split</param>
	// / <param name="entryPoint">The entry point - The start point</param>
	// / <param name="exitPoint">The exit point - The end point</param>
	// / <param name="splitSize">The size of the split. Think of this as the
	// laser-width</param>
	// / <param name="first">The first collection of vertexes</param>
	// / <param name="second">The second collection of vertexes</param>
	public static void SplitShape(Array<Vector2> vertices,
			Vector2 localEntryPoint, Vector2 localExitPoint, float splitSize,
			Array<Vector2> first, Array<Vector2> second) {

		Array<Array<Vector2>> newPolygon = new Array<Array<Vector2>>(2);
		for (int i = 0; i < 2; i++) {
			newPolygon.add(new Array<Vector2>(vertices.size));
		}

		Vector2 tmp1 = new Vector2();
		Vector2 tmp2 = new Vector2();

		int[] cutAdded = { -1, -1 };
		int last = -1;
		for (int i = 0; i < vertices.size; i++) {
			int n;

			tmp1.set(localExitPoint);
			tmp1.sub(localEntryPoint);

			tmp2.set(vertices.get(i));
			tmp2.sub(localEntryPoint);

			if (BayazitDecomposer.Cross(tmp1, 1).dot(tmp2) > BayazitDecomposer.Epsilon)
				n = 0;
			else
				n = 1;

			if (last != n) {
				if (last == 0) {
					// Debug.Assert(cutAdded[0] == -1);
					cutAdded[0] = newPolygon.get(last).size;
					newPolygon.get(last).add(localExitPoint.cpy());
					newPolygon.get(last).add(localEntryPoint.cpy());
				}
				if (last == 1) {
					// Debug.Assert(cutAdded[last] == -1);
					cutAdded[last] = newPolygon.get(last).size;
					newPolygon.get(last).add(localEntryPoint.cpy());
					newPolygon.get(last).add(localExitPoint.cpy());
				}
			}

			newPolygon.get(n).add(vertices.get(i));
			last = n;
		}

		if (cutAdded[0] == -1) {
			cutAdded[0] = newPolygon.get(0).size;
			newPolygon.get(0).add(localExitPoint.cpy());
			newPolygon.get(0).add(localEntryPoint.cpy());
		}
		if (cutAdded[1] == -1) {
			cutAdded[1] = newPolygon.get(1).size;
			newPolygon.get(1).add(localEntryPoint.cpy());
			newPolygon.get(1).add(localExitPoint.cpy());
		}

		for (int n = 0; n < 2; n++) {
			Vector2 offset;
			if (cutAdded[n] > 0) {
				tmp1.set(newPolygon.get(n).get(cutAdded[n] - 1));
				tmp2.set(newPolygon.get(n).get(cutAdded[n]));

				offset = tmp1.sub(tmp2);
			} else {
				tmp1.set(newPolygon.get(n).get(newPolygon.get(n).size - 1));
				tmp2.set(newPolygon.get(n).get(0));

				offset = tmp1.sub(tmp2);
			}
			offset.nor();
			offset.mul(splitSize);

			newPolygon.get(n).get(cutAdded[n]).add(offset);

			if (cutAdded[n] < newPolygon.get(n).size - 2) {
				tmp1.set(newPolygon.get(n).get(cutAdded[n] + 2));
				tmp2.set(newPolygon.get(n).get(cutAdded[n] + 1));

				offset = tmp1.sub(tmp2);
			} else {
				tmp1.set(newPolygon.get(n).get(0));
				tmp2.set(newPolygon.get(n).get(newPolygon.get(n).size - 1));

				offset = tmp1.sub(tmp2);
			}
			offset.nor();
			offset.mul(splitSize);

			newPolygon.get(n).get(cutAdded[n] + 1).add(offset);
		}

		Array<Vector2> array = newPolygon.get(0);
		for (int i = 0; i < array.size; i++) {
			first.add(array.get(i));
		}
		array = newPolygon.get(1);
		for (int i = 0; i < array.size; i++) {
			second.add(array.get(i));
		}
	}

	public static boolean SanityCheck(Array<Vector2> vertices) {

		if (vertices.size < 3)
			return false;

		// gdx/jni/Box2D/Collision/Shapes/b2PolygonShape.cpp, line
		// 134
		// Assertion failed: (2 <= count && count <= 8)
		if (vertices.size > BayazitDecomposer.MaxPolygonVertices) {
			return false;
		}

		float area = BayazitDecomposer.GetSignedArea(vertices);
		if (area < 0.00001f)
			return false;

		Vector2 tmp1 = new Vector2();
		Vector2 tmp2 = new Vector2();

		for (int i = 0; i < vertices.size; ++i) {
			int i1 = i;
			int i2 = i + 1 < vertices.size ? i + 1 : 0;

			tmp1.set(vertices.get(i2));
			tmp2.set(vertices.get(i1));

			Vector2 edge = tmp1.sub(tmp2);
			if (edge.len2() < BayazitDecomposer.Epsilon
					* BayazitDecomposer.Epsilon)
				return false;
		}

		for (int i = 0; i < vertices.size; ++i) {
			int i1 = i;
			int i2 = i + 1 < vertices.size ? i + 1 : 0;

			tmp1.set(vertices.get(i2));
			tmp2.set(vertices.get(i1));

			Vector2 edge = tmp1.sub(tmp2);

			for (int j = 0; j < vertices.size; ++j) {
				// Don't check vertices on the current edge.
				if (j == i1 || j == i2) {
					continue;
				}

				tmp1.set(vertices.get(j));
				tmp2.set(vertices.get(i1));

				Vector2 r = tmp1.sub(tmp2);

				// Your polygon is non-convex (it has an indentation) or
				// has colinear edges.
				float s = edge.x * r.y - edge.y * r.x;

				if (s < 0.0f)
					return false;
			}
		}

		return true;
	}
}

/*
 * 
 * package { import Box2D.Dynamics.*; import Box2D.Collision.*; import
 * Box2D.Collision.Shapes.*; import Box2D.Dynamics.Joints.*; import
 * Box2D.Dynamics.Contacts.*; import Box2D.Common.Math.*; import Box2D.Common.*;
 * 
 * public class Cutter { public static function
 * CheckPolyShape(poly:b2PolygonDef):int { if (!(3 <= poly.vertexCount &&
 * poly.vertexCount <= b2Settings.b2_maxPolygonVertices)) { return -1; }
 * 
 * var m_normals:Array = new Array(poly.vertexCount); var i1:int; var i2:int;
 * 
 * for (var i:int = 0; i < poly.vertexCount; i++) { i1 = i; i2 = i + 1 <
 * poly.vertexCount ? i + 1 : 0; var edge:b2Vec2 = poly.vertices[i2].Copy();
 * edge.Subtract(poly.vertices[i1]);
 * 
 * if (!(edge.LengthSquared() > b2Settings.b2_linearSlop *
 * b2Settings.b2_linearSlop)) { return -1; }
 * 
 * m_normals[i] = b2Math.b2CrossVF(edge, 1); m_normals[i].Normalize(); }
 * 
 * for (i = 0; i < poly.vertexCount; i++) { for (var j = 0; j <
 * poly.vertexCount; j++) { if (j == i || j == (i + 1) % poly.vertexCount) {
 * continue; }
 * 
 * var aDot:b2Vec2 = poly.vertices[j].Copy(); aDot.Subtract(poly.vertices[i]);
 * var s:Number = b2Math.b2Dot(m_normals[i], aDot); if (!(s <
 * -b2Settings.b2_linearSlop)) { return -1; } } }
 * 
 * for (i = 1; i < poly.vertexCount; i++) { var cross:Number =
 * b2Math.b2CrossVV(m_normals[i-1], m_normals[i]);
 * 
 * cross = b2Math.b2Clamp(cross, -1, 1); var angle:Number = Math.asin(cross); if
 * (!(angle > b2Settings.b2_angularSlop)) { return -1; } }
 * 
 * var m_centroid:b2Vec2 = new b2Vec2(); var area:Number = 0; var pRef:b2Vec2 =
 * new b2Vec2(); var inv3:Number = 1 / 3;
 * 
 * for (i = 0; i < poly.vertexCount; i++) { var p1:b2Vec2 = pRef; var p2:b2Vec2
 * = poly.vertices[i]; var p3:b2Vec2 = i + 1 < poly.vertexCount ?
 * poly.vertices[i+1] : poly.vertices[0];
 * 
 * var e1:b2Vec2 = p2.Copy(); e1.Subtract(p1); var e2:b2Vec2 = p3.Copy();
 * e2.Subtract(p1);
 * 
 * var D:Number = b2Math.b2CrossVV(e1,e2); var triangleArea:Number = 0.5 * D;
 * area += triangleArea;
 * 
 * var p123:b2Vec2 = p1.Copy(); p123.Add(p2); p123.Add(p3); var arInv3:Number =
 * triangleArea * inv3; p123.Multiply(arInv3); m_centroid.Add(p123); }
 * 
 * if (!(area > b2Settings.b2_linearSlop)) { return -1; } m_centroid.Multiply(1
 * / area);
 * 
 * for (i = 0; i < poly.vertexCount; i++) { i1 = i - 1 >= 0 ? i - 1 :
 * poly.vertexCount - 1; i2 = i;
 * 
 * var n1:b2Vec2 = m_normals[i1]; var n2:b2Vec2 = m_normals[i2]; var v:b2Vec2 =
 * poly.vertices[i].Copy(); v.Subtract(m_centroid);
 * 
 * var d:b2Vec2 = new b2Vec2(); d.x = b2Math.b2Dot(n1, v) -
 * b2Settings.b2_toiSlop; d.y = b2Math.b2Dot(n2, v) - b2Settings.b2_toiSlop;
 * 
 * if (!(d.x >= 0)) { return -1; }
 * 
 * if (!(d.y >= 0)) { return -1; } }
 * 
 * return 0; }
 * 
 * public static function SplitShape(shape:b2PolygonShape, segment:b2Segment,
 * splitSize:Number, newPolygon:Array):int { var lambda:Array = [1]; var
 * normal:b2Vec2 = new b2Vec2();
 * 
 * var b:b2Body = shape.GetBody(); var xf:b2XForm = b.GetXForm();
 * 
 * if (shape.TestSegment(xf, lambda, normal, segment, 1) !=
 * b2Shape.e_hitCollide) { return -1; }
 * 
 * var entryPoint:b2Vec2 = segment.p1.Copy(); entryPoint.Multiply(1 -
 * lambda[0]); var tmp:b2Vec2 = segment.p2.Copy(); tmp.Multiply(lambda[0]);
 * entryPoint.Add(tmp);
 * 
 * var reverseSegment:b2Segment = new b2Segment(); reverseSegment.p1 =
 * segment.p2; reverseSegment.p2 = segment.p1;
 * 
 * if (shape.TestSegment(xf, lambda, normal, reverseSegment, 1) !=
 * b2Shape.e_hitCollide) { return -1; }
 * 
 * var exitPoint:b2Vec2 = reverseSegment.p1.Copy(); exitPoint.Multiply(1 -
 * lambda[0]); tmp = reverseSegment.p2.Copy(); tmp.Multiply(lambda[0]);
 * exitPoint.Add(tmp);
 * 
 * var localEntryPoint:b2Vec2 = b.GetLocalPoint(entryPoint); var
 * localExitPoint:b2Vec2 = b.GetLocalPoint(exitPoint);
 * 
 * var vertices:Array = shape.GetVertices(); var cutAdded:Array = [-1,-1]; var
 * last:int = -1;
 * 
 * for (var i:int = 0; i < shape.GetVertexCount(); i++) { var n:int;
 * 
 * var subExitP:b2Vec2 = localExitPoint.Copy();
 * subExitP.Subtract(localEntryPoint); var subVertex:b2Vec2 =
 * vertices[i].Copy(); subVertex.Subtract(localEntryPoint); if
 * (b2Math.b2Dot(b2Math.b2CrossVF(subExitP, 1), subVertex) > 0) { n = 0; }
 * 
 * else { n = 1; }
 * 
 * if (last != n) { if (last == 0) { cutAdded[0] = newPolygon[last].vertexCount;
 * newPolygon[last].vertices[newPolygon[last].vertexCount] = localExitPoint;
 * newPolygon[last].vertexCount++;
 * newPolygon[last].vertices[newPolygon[last].vertexCount] = localEntryPoint;
 * newPolygon[last].vertexCount++; }
 * 
 * if (last == 1) { cutAdded[last] = newPolygon[last].vertexCount;
 * newPolygon[last].vertices[newPolygon[last].vertexCount] = localEntryPoint;
 * newPolygon[last].vertexCount++;
 * newPolygon[last].vertices[newPolygon[last].vertexCount] = localExitPoint;
 * newPolygon[last].vertexCount++; } }
 * newPolygon[n].vertices[newPolygon[n].vertexCount] = vertices[i];
 * newPolygon[n].vertexCount++; last = n; }
 * 
 * if (cutAdded[0] == -1) { cutAdded[0] = newPolygon[0].vertexCount;
 * newPolygon[0].vertices[newPolygon[0].vertexCount] = localEntryPoint;
 * newPolygon[0].vertexCount++;
 * newPolygon[0].vertices[newPolygon[0].vertexCount] = localExitPoint;
 * newPolygon[0].vertexCount++; }
 * 
 * if (cutAdded[1] == -1) { cutAdded[1] = newPolygon[1].vertexCount;
 * newPolygon[1].vertices[newPolygon[1].vertexCount] = localEntryPoint;
 * newPolygon[1].vertexCount++;
 * newPolygon[1].vertices[newPolygon[1].vertexCount] = localExitPoint;
 * newPolygon[1].vertexCount++; }
 * 
 * for (n = 0; n < 2; n++) { var offset:b2Vec2 = new b2Vec2(); if (cutAdded[n] >
 * 0) { offset = newPolygon[n].vertices[cutAdded[n]-1].Copy();
 * offset.Subtract(newPolygon[n].vertices[cutAdded[n]]); }
 * 
 * else { offset = newPolygon[n].vertices[newPolygon[n].vertexCount-1].Copy();
 * offset.Subtract(newPolygon[n].vertices[0]); }
 * 
 * offset.Normalize(); offset.Multiply(splitSize);
 * newPolygon[n].vertices[cutAdded[n]].Add(offset);
 * 
 * if (cutAdded[n] < newPolygon[n].vertexCount-2) { offset =
 * newPolygon[n].vertices[cutAdded[n]+2].Copy();
 * offset.Subtract(newPolygon[n].vertices[cutAdded[n]+1]); }
 * 
 * else { offset = newPolygon[n].vertices[0].Copy();
 * offset.Subtract(newPolygon[n].vertices[newPolygon[n].vertexCount-1]); }
 * 
 * offset.Normalize(); offset.Multiply(splitSize);
 * 
 * newPolygon[n].vertices[cutAdded[n]+1].Add(offset); }
 * 
 * for (n = 0; n < 2; n++) { if (CheckPolyShape(newPolygon[n])) { return -1; } }
 * 
 * return 0; } } }
 */