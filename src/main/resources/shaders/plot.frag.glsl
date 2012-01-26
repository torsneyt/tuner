
#version 120

varying float respValue;
varying float centerSqDist;
varying vec2 vertexSqDist;
varying vec2 theta;

void main() {
  vec2 weightedDist = theta * vertexSqDist;
  float ttlDist = centerSqDist + vertexSqDist.x + vertexSqDist.y;
  float alpha = respValue * exp(-ttlDist);
  //float alpha = 0.4;
  //float alpha = 1.0;
  gl_FragColor = vec4(1.0, 0.0, 0.0, alpha);
}

