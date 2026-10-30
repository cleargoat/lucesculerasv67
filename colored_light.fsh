#version 150

uniform sampler2D DepthSampler;
uniform mat4 InvProj;
uniform mat4 InvView;
uniform vec3 LightPos;    // posicion de la luz relativa a la camara
uniform vec4 LightColor;  // rgb = color * fuerza, a = radio en bloques

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 1.0) discard;                       // cielo: sin luz

    vec4 ndc = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPos = InvProj * ndc;
    viewPos /= viewPos.w;
    vec3 worldRel = (InvView * vec4(viewPos.xyz, 1.0)).xyz;

    float dist = length(worldRel - LightPos);
    float f = 1.0 - clamp(dist / LightColor.a, 0.0, 1.0);
    f *= f;                                          // caida suave
    if (f <= 0.0) discard;

    fragColor = vec4(LightColor.rgb * f, 1.0);       // sin normales, sin especular, sin sombras
}
