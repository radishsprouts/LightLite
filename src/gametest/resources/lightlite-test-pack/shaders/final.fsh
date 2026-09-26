#version 120

uniform sampler2D colortex0;
varying vec2 texcoord;

// Pass-through: whatever the gbuffers wrote to colortex0 is what ends up on screen
void main() {
    gl_FragColor = texture2D(colortex0, texcoord);
}
