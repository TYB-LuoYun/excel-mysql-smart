/*
 Navicat Premium Data Transfer

 Source Server         : rm-bp-aliyuncs(statistics_ro)
 Source Server Type    : MySQL
 Source Server Version : 80034
 Source Host           : rm-bp17dx697cf6cwpchdo.mysql.rds.aliyuncs.com:3306
 Source Schema         : statiscs

 Target Server Type    : MySQL
 Target Server Version : 80034
 File Encoding         : 65001

 Date: 30/08/2024 13:47:49
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for excel_mysql
-- ----------------------------
DROP TABLE IF EXISTS `excel_mysql`;
CREATE TABLE `excel_mysql`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `table_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '表名',
  `excel_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '表格名',
  `excel_head_row_num` int(0) NULL DEFAULT 1 COMMENT '表格行数',
  `sheet_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'sheet名',
  `sheet_index` int(0) NULL DEFAULT 0,
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL,
  `delete_flag` tinyint(0) NULL DEFAULT 0 COMMENT '软删除标记',
  `create_by` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
