/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.ozone.om.helpers;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.ozone.om.OMConfigKeys;
import org.apache.hadoop.util.StringUtils;

import javax.annotation.Nonnull;
import java.nio.file.Paths;
import java.util.Map;

import static org.apache.hadoop.ozone.OzoneConsts.OM_KEY_PREFIX;
import static org.apache.hadoop.ozone.OzoneConsts.OZONE_URI_DELIMITER;

/**
 * Utility class for OzoneFileSystem.
 */
public final class OzoneFSUtils {

  private OzoneFSUtils() {}

  /**
   * Returns string representation of path after removing the leading slash.
   */
  public static String pathToKey(Path path) {
    return path.toString().substring(1);
  }

  /**
   * Returns string representation of the input path parent. The function adds
   * a trailing slash if it does not exist and returns an empty string if the
   * parent is root.
   */
  public static String getParent(String keyName) {
    java.nio.file.Path parentDir = Paths.get(keyName).getParent();
    if (parentDir == null) {
      return "";
    }
    return addTrailingSlashIfNeeded(parentDir.toString());
  }

  /**
   * The function returns immediate child of given ancestor in a particular
   * descendant. For example if ancestor is /a/b and descendant is /a/b/c/d/e
   * the function should return /a/b/c/. If the descendant itself is the
   * immediate child then it is returned as is without adding a trailing slash.
   * This is done to distinguish files from a directory as in ozone files do
   * not carry a trailing slash.
   */
  public static String getImmediateChild(String descendant, String ancestor) {
    ancestor =
        !ancestor.isEmpty() ? addTrailingSlashIfNeeded(ancestor) : ancestor;
    if (!descendant.startsWith(ancestor)) {
      return null;
    }
    java.nio.file.Path descendantPath = Paths.get(descendant);
    java.nio.file.Path ancestorPath = Paths.get(ancestor);
    int ancestorPathNameCount =
        ancestor.isEmpty() ? 0 : ancestorPath.getNameCount();
    if (descendantPath.getNameCount() - ancestorPathNameCount > 1) {
      return addTrailingSlashIfNeeded(
          ancestor + descendantPath.getName(ancestorPathNameCount));
    }
    return descendant;
  }

  public static String addTrailingSlashIfNeeded(String key) {
    if (!key.endsWith(OZONE_URI_DELIMITER)) {
      return key + OZONE_URI_DELIMITER;
    } else {
      return key;
    }
  }

  public static boolean isFile(String keyName) {
    return !keyName.endsWith(OZONE_URI_DELIMITER);
  }

  /**
   * Whether the pathname is valid.  Currently prohibits relative paths,
   * names which contain a ":" or "//", or other non-canonical paths.
   */
  public static boolean isValidName(String src) {
    // Path must be absolute.
    if (!src.startsWith(Path.SEPARATOR)) {
      return false;
    }

    // Check for ".." "." ":" "/"
    String[] components = StringUtils.split(src, '/');
    for (int i = 0; i < components.length; i++) {
      String element = components[i];
      if (element.equals(".")  ||
          (element.contains(":"))  ||
          (element.contains("/") || element.equals(".."))) {
        return false;
      }
      // The string may start or end with a /, but not have
      // "//" in the middle.
      if (element.isEmpty() && i != components.length - 1 &&
          i != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * The function returns leaf node name from the given absolute path. For
   * example, the given key path '/a/b/c/d/e/file1' then it returns leaf node
   * name 'file1'.
   */
  public static String getFileName(@Nonnull String keyName) {
    java.nio.file.Path fileName = Paths.get(keyName).getFileName();
    if (fileName != null) {
      return fileName.toString();
    }
    // failed to converts a path key
    return keyName;
  }

  /**
   * Verifies whether the childKey is an immediate path under the given
   * parentKey.
   *
   * @param parentKey parent key name
   * @param childKey  child key name
   * @return true if childKey is an immediate path under the given parentKey
   */
  public static boolean isImmediateChild(String parentKey, String childKey) {

    // Empty childKey has no parent, so just returning false.
    if (org.apache.commons.lang3.StringUtils.isBlank(childKey)) {
      return false;
    }
    java.nio.file.Path parentPath = Paths.get(parentKey);
    java.nio.file.Path childPath = Paths.get(childKey);

    java.nio.file.Path childParent = childPath.getParent();
    // Following are the valid parentKey formats:
    // parentKey="" or parentKey="/" or parentKey="/a" or parentKey="a"
    // Following are the valid childKey formats:
    // childKey="/" or childKey="/a/b" or childKey="a/b"
    if (org.apache.commons.lang3.StringUtils.isBlank(parentKey)) {
      return childParent == null ||
              OM_KEY_PREFIX.equals(childParent.toString());
    }

    return parentPath.equals(childParent);
  }

  /**
   * The function returns parent directory from the given absolute path. For
   * example, the given key path '/a/b/c/d/e/file1' then it returns parent
   * directory name as 'e'.
   *
   * @param keyName key name
   * @return parent directory. If not found then return keyName itself.
   */
  public static String getParentDir(@Nonnull String keyName) {
    java.nio.file.Path fileName = Paths.get(keyName).getParent();
    if (fileName != null) {
      return fileName.toString();
    }
    // failed to find a parent directory.
    return "";
  }

  /**
   * This function appends the given file name to the given key name path.
   *
   * @param keyName key name
   * @param fileName  file name
   * @return full path
   */
  public static String appendFileNameToKeyPath(String keyName,
                                               String fileName) {
    StringBuilder newToKeyName = new StringBuilder(keyName);
    newToKeyName.append(OZONE_URI_DELIMITER);
    newToKeyName.append(fileName);
    return newToKeyName.toString();
  }

  /**
   * Returns the number of path components in the given keyName.
   *
   * @param keyName keyname
   * @return path components count
   */
  public static int getFileCount(String keyName) {
    java.nio.file.Path keyPath = Paths.get(keyName);
    return keyPath.getNameCount();
  }


  /**
   * Returns true if the bucket is FS Optimised.
   * @param bucketMetadata
   * @return
   */
  public static boolean isFSOptimizedBucket(
      Map<String, String> bucketMetadata) {
    // layout version V1 represents optimized FS path
    boolean layoutVersionEnabled =
        org.apache.commons.lang3.StringUtils.equalsIgnoreCase(
            OMConfigKeys.OZONE_OM_LAYOUT_VERSION_V1,
            bucketMetadata
                .get(OMConfigKeys.OZONE_OM_LAYOUT_VERSION));

    boolean fsEnabled =
        Boolean.parseBoolean(bucketMetadata
            .get(OMConfigKeys.OZONE_OM_ENABLE_FILESYSTEM_PATHS));

    return layoutVersionEnabled && fsEnabled;
  }

  public static String removeTrailingSlashIfNeeded(String key) {
    if (key.endsWith(OZONE_URI_DELIMITER)) {
      java.nio.file.Path keyPath = Paths.get(key);
      return keyPath.toString();
    } else {
      return key;
    }
  }
}
