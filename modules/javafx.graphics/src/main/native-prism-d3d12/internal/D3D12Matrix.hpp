/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#pragma once

namespace D3D12 {
namespace Internal {

/**
 * 4x4 Matrix template for Matrix operations.
 *
 * This mostly mimicks how D3D9's D3DMATRIX was created
 * and on top of usual overloads adds operations which
 * were used by D3D9 backend via D3DUtils_* calls in D3DContext.cc
 */
template <typename T>
class Matrix
{
    union {
        struct {
            T _11, _12, _13, _14;
            T _21, _22, _23, _24;
            T _31, _32, _33, _34;
            T _41, _42, _43, _44;

        };
        T m[4][4];
        T a[16];
    };

public:
    static const Matrix<T> IDENTITY;

    Matrix()
    {
        SetToIdentity();
    }

    Matrix(T a[16])
    {
        for (int i = 0; i < 16; ++i)
        {
            this->a[i] = a[i];
        }
    }

    Matrix(T m00, T m01, T m02, T m03,
           T m10, T m11, T m12, T m13,
           T m20, T m21, T m22, T m23,
           T m30, T m31, T m32, T m33)
    {
        m[0][0] = m00; m[0][1] = m01; m[0][2] = m02; m[0][3] = m03;
        m[1][0] = m10; m[1][1] = m11; m[1][2] = m12; m[1][3] = m13;
        m[2][0] = m20; m[2][1] = m21; m[2][2] = m22; m[2][3] = m23;
        m[3][0] = m30; m[3][1] = m31; m[3][2] = m32; m[3][3] = m33;
    }

    Matrix(const Matrix<T>& other)
    {
        for (int i = 0; i < 16; ++i)
        {
            this->a[i] = other.a[i];
        }
    }

    Matrix(Matrix<T>&& other)
    {
        for (int i = 0; i < 16; ++i)
        {
            this->a[i] = other.a[i];
        }
    }

    Matrix<T>& operator=(const Matrix<T>& other)
    {
        for (int i = 0; i < 16; ++i)
        {
            this->a[i] = other.a[i];
        }

        return *this;
    }

    Matrix<T>& operator=(Matrix<T>&& other)
    {
        for (int i = 0; i < 16; ++i)
        {
            this->a[i] = other.a[i];
        }

        return *this;
    }

    Matrix<T> operator+(const Matrix<T>& other) const
    {
        Matrix<T> ret;

        for (int i = 0; i < 16; ++i)
        {
            ret->a[i] = this->a[i] + other.a[i];
        }

        return ret;
    }

    Matrix<T> operator-(const Matrix<T>& other) const
    {
        Matrix<T> ret;

        for (int i = 0; i < 16; ++i)
        {
            ret->a[i] = this->a[i] - other.a[i];
        }

        return ret;
    }

    Matrix<T>& operator+=(const Matrix<T>& other)
    {
        for (int i = 0; i < 16; ++i)
        {
            this->a[i] += other.a[i];
        }

        return *this;
    }

    Matrix<T>& operator-=(const Matrix<T>& other)
    {
        for (int i = 0; i < 16; ++i)
        {
            this->a[i] -= other.a[i];
        }

        return *this;
    }

    Matrix<T> Transpose() const
    {
        Matrix<T> ret;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                ret.m[j][i] = this->m[i][j];
            }
        }

        return ret;
    }

    Matrix<T> Mul(const Matrix<T>& other) const
    {
        Matrix<T> ret;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                float t = 0;
                for (int k = 0; k < 4; k++) {
                    t += this->m[i][k] * other.m[k][j];
                }
                ret.m[i][j] = t;
            }
        }

        return ret;
    }

    Matrix<T> MulTranspose(const Matrix<T>& other) const
    {
        Matrix<T> ret;

        for (int i=0; i<4; i++) {
            for (int j=0; j<4; j++) {
                float t = 0;
                for (int k=0; k<4; k++) {
                    t += this->m[i][k] * other.m[k][j];
                }

                // transpose after multiplying
                ret.m[j][i] = t;
            }
        }

        return ret;
    }

    Coords_XYZW_FLOAT Mul(const Coords_XYZW_FLOAT& other) const
    {
        float v[4] = { other.x, other.y, other.z, other.w };
        float ret[4];

        for (int i=0; i<4; i++) {
            float t = 0.0f;
            for (int k=0; k<4; k++) {
                t += this->m[i][k] * v[k];
            }

            ret[i] = t;
        }

        return Coords_XYZW_FLOAT{ret[0], ret[1], ret[2], ret[3]};
    }

    void SetToIdentity()
    {
        m[0][1] = m[0][2] = m[0][3] = 0.0f;
        m[1][0] = m[1][2] = m[1][3] = 0.0f;
        m[2][0] = m[2][1] = m[2][3] = 0.0f;
        m[3][0] = m[3][1] = m[3][2] = 0.0f;
        m[0][0] = m[1][1] = m[2][2] = m[3][3] = 1.0f;
    }

    const T* Data() const
    {
        return a;
    }
};

template <typename T> const Matrix<T> Matrix<T>::IDENTITY;

} // namespace Internal
} // namespace D3D12
