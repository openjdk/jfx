#!/usr/bin/env python3

'''
/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
'''
'''
Tool to generate base64 code
'''
import os
import base64
import sys

class ImageProcessor:
    def __init__(self, directory_path):
        self.directory_path = directory_path

    def read_images(self):
        image_data = {}
        for filename in os.listdir(self.directory_path):
            if filename.lower().endswith(('.png', '.svg')):
                with open(os.path.join(self.directory_path, filename), 'rb') as image_file:
                    image_data[filename] = image_file.read()
        return image_data

    def generate_base64(self, image_data):
        base64_images = {}
        for filename, data in image_data.items():
            base64_images[filename] = base64.b64encode(data).decode('utf-8')
        return base64_images

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python script_name.py directory_path")
        sys.exit(1)

    image_directory = sys.argv[1]

    if not os.path.isdir(image_directory):
        print("Invalid directory path.")
        sys.exit(1)

    image_processor = ImageProcessor(image_directory)

    images = image_processor.read_images()
    base64_images = image_processor.generate_base64(images)
    size_of_list = len(images)
    print("imageMap = {")
    for filename, base64_code in base64_images.items():
        file_name_without_extension = os.path.splitext(filename)[0]
        print("{"+file_name_without_extension +"_s, " +  base64_code + "_s},")
    print("};")
