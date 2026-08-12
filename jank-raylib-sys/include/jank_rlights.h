/*
 * jank_rlights.h - raylib's examples/shaders/rlights.h adapted for jank.
 *
 * Why this exists: jank functions can neither take nor return native
 * values (Light, Shader members, ...), so the lights helper cannot be a
 * jank namespace. Instead the Light array lives here as module-local
 * state behind index-based wrapper functions with scalar parameters -
 * everything a jank example needs to speak to raylib's lighting shaders
 * (lighting.vs/lighting.fs, fog.fs, ...).
 *
 * Shipped by jank-raylib-sys (see jank-build.bb's second include-dir
 * directive); consumers reach it with (:include "raylib.h" "jank_rlights.h").
 *
 * Everything is `static`: each jank module that includes this header gets
 * its own private light state (raylib examples are one module each, so in
 * practice there is exactly one copy per binary).
 *
 * CreateLight/UpdateLightValues are kept verbatim from rlights.h
 * (Copyright (c) 2017-2024 Victor Fisac (@victorfisac) and Ramon
 * Santamaria (@raysan5), zlib/libpng license), only marked static.
 *
 * Also included: per-type scalar uniform setters (the staging pattern from
 * docs/guide/jank-interop-lessons.md, "prefer per-type scalar C setters"),
 * so lighting examples need no per-file cpp/raw shim block.
 */

#ifndef JANK_RLIGHTS_H
#define JANK_RLIGHTS_H

#include "raylib.h"

#define MAX_LIGHTS 4 // Max dynamic lights supported by shader

// Light data
typedef struct {
    int type;
    bool enabled;
    Vector3 position;
    Vector3 target;
    Color color;
    float attenuation;

    // Shader locations
    int enabledLoc;
    int typeLoc;
    int positionLoc;
    int targetLoc;
    int colorLoc;
    int attenuationLoc;
} Light;

// Light type
typedef enum {
    LIGHT_DIRECTIONAL = 0,
    LIGHT_POINT
} LightType;

static int lightsCount = 0; // Current amount of created lights

// Send light properties to shader (verbatim rlights.h, marked static)
static void UpdateLightValues(Shader shader, Light light)
{
    SetShaderValue(shader, light.enabledLoc, &light.enabled, SHADER_UNIFORM_INT);
    SetShaderValue(shader, light.typeLoc, &light.type, SHADER_UNIFORM_INT);

    float position[3] = { light.position.x, light.position.y, light.position.z };
    SetShaderValue(shader, light.positionLoc, position, SHADER_UNIFORM_VEC3);

    float target[3] = { light.target.x, light.target.y, light.target.z };
    SetShaderValue(shader, light.targetLoc, target, SHADER_UNIFORM_VEC3);

    float color[4] = { (float)light.color.r/(float)255, (float)light.color.g/(float)255,
                       (float)light.color.b/(float)255, (float)light.color.a/(float)255 };
    SetShaderValue(shader, light.colorLoc, color, SHADER_UNIFORM_VEC4);
}

// Create a light and get shader locations (verbatim rlights.h, marked static)
static Light CreateLight(int type, Vector3 position, Vector3 target, Color color, Shader shader)
{
    Light light = { 0 };

    if (lightsCount < MAX_LIGHTS)
    {
        light.enabled = true;
        light.type = type;
        light.position = position;
        light.target = target;
        light.color = color;

        // NOTE: Lighting shader naming must be the provided ones
        light.enabledLoc = GetShaderLocation(shader, TextFormat("lights[%i].enabled", lightsCount));
        light.typeLoc = GetShaderLocation(shader, TextFormat("lights[%i].type", lightsCount));
        light.positionLoc = GetShaderLocation(shader, TextFormat("lights[%i].position", lightsCount));
        light.targetLoc = GetShaderLocation(shader, TextFormat("lights[%i].target", lightsCount));
        light.colorLoc = GetShaderLocation(shader, TextFormat("lights[%i].color", lightsCount));

        UpdateLightValues(shader, light);

        lightsCount++;
    }

    return light;
}

/* ---- jank-facing wrappers: index-based, scalar parameters only ---- */

static Light jank_rl_lights[MAX_LIGHTS];

/* Create light i (returns its index; -1 when MAX_LIGHTS reached). */
static int jank_rl_create_light(int type,
                                double px, double py, double pz,
                                double tx, double ty, double tz,
                                int r, int g, int b, int a,
                                Shader shader)
{
    if (lightsCount >= MAX_LIGHTS) return -1;
    int i = lightsCount;
    jank_rl_lights[i] = CreateLight(type,
                                    (Vector3){ (float)px, (float)py, (float)pz },
                                    (Vector3){ (float)tx, (float)ty, (float)tz },
                                    (Color){ (unsigned char)r, (unsigned char)g,
                                             (unsigned char)b, (unsigned char)a },
                                    shader);
    return i;
}

static void jank_rl_update_light(Shader shader, int i)
{
    UpdateLightValues(shader, jank_rl_lights[i]);
}

static void jank_rl_toggle_light(int i)
{
    jank_rl_lights[i].enabled = !jank_rl_lights[i].enabled;
}

/* Move light i (send with jank_rl_update_light afterwards). */
static void jank_rl_set_light_pos(int i, double x, double y, double z)
{
    jank_rl_lights[i].position = (Vector3){ (float)x, (float)y, (float)z };
}

static int jank_rl_light_enabled(int i)
{
    return jank_rl_lights[i].enabled ? 1 : 0;
}

/* ---- general shader helpers shared by the lighting examples ---- */

/* shader.locs[idx] = loc (e.g. idx = SHADER_LOC_VECTOR_VIEW) */
static void jank_rl_shader_set_loc(Shader* s, int idx, int loc)
{
    s->locs[idx] = loc;
}

static void jank_rl_set_int(Shader s, int loc, int v)
{
    SetShaderValue(s, loc, &v, SHADER_UNIFORM_INT);
}

static void jank_rl_set_float(Shader s, int loc, double v)
{
    float f = (float)v;
    SetShaderValue(s, loc, &f, SHADER_UNIFORM_FLOAT);
}

static void jank_rl_set_vec2(Shader s, int loc, double a, double b)
{
    float v[2] = { (float)a, (float)b };
    SetShaderValue(s, loc, v, SHADER_UNIFORM_VEC2);
}

static void jank_rl_set_vec3(Shader s, int loc, double a, double b, double c)
{
    float v[3] = { (float)a, (float)b, (float)c };
    SetShaderValue(s, loc, v, SHADER_UNIFORM_VEC3);
}

static void jank_rl_set_vec4(Shader s, int loc, double a, double b, double c, double d)
{
    float v[4] = { (float)a, (float)b, (float)c, (float)d };
    SetShaderValue(s, loc, v, SHADER_UNIFORM_VEC4);
}

/* viewPos pushed straight from the camera (the raymarching shim pattern) */
static void jank_rl_push_view_pos(Shader s, int loc, Camera3D* c)
{
    float pos[3] = { c->position.x, c->position.y, c->position.z };
    SetShaderValue(s, loc, pos, SHADER_UNIFORM_VEC3);
}

#endif // JANK_RLIGHTS_H
