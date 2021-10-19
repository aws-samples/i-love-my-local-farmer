/*
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.ilmlf.product.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Class for hashing Lambda function code. This is done because certain environment variables passed to the functions
 * can change the hash if they are listed in different orders due to Map implementations, thereby causing unnecessary updates
 * to the Lambdas in an account.
 */
public class Hashing {
  public static String hashDirectory(String directoryPath, boolean includeHiddenFiles)
      throws IOException {
    File directory = new File(directoryPath);

    if (!directory.isDirectory()) {
      throw new IllegalArgumentException("Not a directory");
    }

    Vector<FileInputStream> fileStreams = new Vector<>();
    collectFiles(directory, fileStreams, includeHiddenFiles);

    try (SequenceInputStream sequenceInputStream =
        new SequenceInputStream(fileStreams.elements())) {
      return DigestUtils.md5Hex(sequenceInputStream);
    }
  }

  private static void collectFiles(
      File directory, List<FileInputStream> fileInputStreams, boolean includeHiddenFiles)
      throws IOException {
    File[] files = directory.listFiles();

    if (files != null) {
      Arrays.sort(files, Comparator.comparing(File::getName));

      for (File file : files) {
        if (includeHiddenFiles || !Files.isHidden(file.toPath())) {
          if (file.isDirectory()) {
            collectFiles(file, fileInputStreams, includeHiddenFiles);
          } else {
            fileInputStreams.add(new FileInputStream(file));
          }
        }
      }
    }
  }
}
