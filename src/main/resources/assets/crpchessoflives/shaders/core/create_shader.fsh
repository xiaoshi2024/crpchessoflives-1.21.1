#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float EntityPositionY;
uniform float EntityHeight;
uniform float Percentage;
uniform float GuiScale;
uniform int PackedRGBColor;

in vec3 fragPos;
in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

vec3 unpackRGB8(int rgb) {
    float r = float((rgb >> 16) & 0xFF) / 255.0;
    float g = float((rgb >> 8) & 0xFF) / 255.0;
    float b = float(rgb & 0xFF) / 255.0;
    return vec3(r, g, b);
}

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }

    float cutoff = EntityPositionY + EntityHeight * (Percentage / 100.0);
    if (fragPos.y >= cutoff) {
        discard;
    }

    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;

    if (fragPos.y >= cutoff - 0.05 * GuiScale &&
        fragPos.y <= cutoff) {
        color.rgb = unpackRGB8(PackedRGBColor);
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
