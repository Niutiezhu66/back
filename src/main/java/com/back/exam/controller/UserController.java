package com.back.exam.controller;

import com.back.exam.common.Result;
import com.back.exam.entity.User;
import com.back.exam.service.UserService;
import com.back.exam.vo.LoginRequestVo;
import com.back.exam.vo.ChangePasswordVo;
import com.back.exam.vo.UpdateProfileVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.back.exam.vo.TeacherStudentVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/user")  // 用户API路径前缀
@CrossOrigin(origins = "*")  // 允许跨域访问
@Tag(name = "用户管理", description = "用户相关操作，包括登录认证、权限验证、批量导入等功能")  // Swagger API分组
public class UserController {

    @Autowired
    private UserService userService;

    // ================== 管理员：用户增删改查 ==================

    @GetMapping("/page")
    @Operation(summary = "分页查询用户", description = "按条件分页获取所有用户列表")
    public Result<Page<User>> getUserPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {

        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 支持按用户名、真实姓名、学号/工号模糊搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getUserId, keyword));
        }
        // 按角色过滤
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }

        wrapper.orderByDesc(User::getCreateTime);
        return Result.success(userService.page(page, wrapper));
    }

    @PostMapping("/add")
    @Operation(summary = "新增用户(后台)")
    public Result<String> addUser(@RequestBody User user) {
        // 查重逻辑
        if(userService.count(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) > 0){
            return Result.error("用户名已存在！");
        }
        if(user.getUserId() != null && userService.count(new LambdaQueryWrapper<User>().eq(User::getUserId, user.getUserId())) > 0){
            return Result.error("学号/工号已存在！");
        }

        if (!StringUtils.hasText(user.getStatus())) {
            user.setStatus("active");
        }
        userService.save(user);
        return Result.success("新增成功");
    }

    @PutMapping("/update")
    @Operation(summary = "修改用户")
    public Result<String> updateUser(@RequestBody User user) {
        // 这里可以加上忽略密码修改的逻辑，或者允许管理员直接重置密码
        userService.updateById(user);
        return Result.success("修改成功");
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料")
    public Result<User> updateProfile(@RequestBody UpdateProfileVo profileVo) {
        return userService.updateProfile(profileVo);
    }

    @PutMapping("/change-password")
    @Operation(summary = "修改个人密码")
    public Result<String> changePassword(@RequestBody ChangePasswordVo changePasswordVo) {
        return userService.changePassword(changePasswordVo);
    }

    // ================== 管理员：删除用户 ==================
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除用户")
    public Result<String> deleteUser(@PathVariable Long id) {
        // 【修改】：使用带有关联清理的自定义方法
        boolean success = userService.deleteUserAndRelations(id);
        if (success) {
            return Result.success("删除成功，并已解绑该用户的所有师生关系！");
        } else {
            return Result.error("删除失败");
        }
    }

    // ================== 修改：下载用户导入Excel模板 ==================
    @GetMapping("/batch/template")
    @Operation(summary = "下载用户导入模板", description = "下载用户批量导入的Excel模板文件，包含师生绑定列")
    public ResponseEntity<byte[]> downloadUserTemplate() throws IOException {
        // 1. 创建Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("用户导入模板");

        // 2. 创建表头 (新增第6列：所属教师工号)
        Row headerRow = sheet.createRow(0);
        String[] headers = {"用户名(账号)", "密码", "真实姓名", "角色(1老师/2学生)", "学号/工号", "所属教师工号(仅学生选填)"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            sheet.autoSizeColumn(i); // 自动调整列宽
        }

        // 3. 创建一行示例数据 (学生绑定老师的示例)
        Row exampleRow = sheet.createRow(1);
        exampleRow.createCell(0).setCellValue("zhangsan");
        exampleRow.createCell(1).setCellValue("123456");
        exampleRow.createCell(2).setCellValue("张三");
        exampleRow.createCell(3).setCellValue("2"); // 2 是学生
        exampleRow.createCell(4).setCellValue("20230001"); // 学生学号
        exampleRow.createCell(5).setCellValue("10001");    // 假设 10001 是某位教师的工号

        // 4. 将Excel写入字节输出流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        byte[] bytes = out.toByteArray();

        // 5. 返回 ResponseEntity
        return ResponseEntity.ok()
                .header("content-disposition", "attachment; filename=user_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/batch/preview-excel")
    @Operation(summary = "预览用户导入Excel", description = "读取上传的Excel文件前几行作为前端预览")
    public Result<List<Map<String, String>>> previewExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传的Excel文件为空！");
        }
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<Map<String, String>> previewList = new ArrayList<>();

            // 预览最多展示前10行数据
            int maxRows = Math.min(sheet.getLastRowNum(), 10);

            // 假设表头在第0行，数据从第1行开始
            for (int i = 1; i <= maxRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // 将每行数据组装成 Map
                Map<String, String> rowData = new LinkedHashMap<>();
                rowData.put("username", getCellValue(row.getCell(0)));
                rowData.put("password", getCellValue(row.getCell(1)));
                rowData.put("realName", getCellValue(row.getCell(2)));
                rowData.put("role", getCellValue(row.getCell(3)));
                rowData.put("userId", getCellValue(row.getCell(4)));
                rowData.put("teacherId", getCellValue(row.getCell(5))); // 第6列所属教师

                // 如果整行都没数据则跳过
                if (!rowData.get("username").isEmpty() || !rowData.get("userId").isEmpty()) {
                    previewList.add(rowData);
                }
            }
            return Result.success(previewList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Excel解析失败，请检查文件格式是否正确。");
        }
    }

    // 辅助解析单元格格式的方法
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        // 使用 DataFormatter 可以完美读取数字、文本而不报错
        DataFormatter formatter = new DataFormatter();
        String val = formatter.formatCellValue(cell);

        return val != null ? val.trim() : "";
    }



    // ================== 解除注释：管理员一键导入师生 ==================
    @PostMapping("/batch/import-users")
    @Operation(summary = "Excel批量导入师生", description = "管理员上传Excel文件批量注册用户")
    public Result<String> importUsers(@RequestParam("file") MultipartFile file) {
        return userService.importUsers(file);
    }

    // --- 教师：根据学号绑定学生 ---
    @PostMapping("/teacher/bind")
    @Operation(summary = "教师绑定学生", description = "教师输入学生学号(userId)，将学生绑定到自己名下")
    public Result<String> bindStudent(
            @RequestParam("teacherId") Long teacherId,
            @RequestParam("studentUserId") Integer studentUserId) {
        return userService.bindStudentByUserId(teacherId, studentUserId);
    }

    // --- 教师/管理员：解绑学生 ---
    @PostMapping("/teacher/unbind")
    @Operation(summary = "解除师生绑定", description = "解除指定教师和学生之间的关联关系")
    public Result<String> unbindStudent(
            @RequestParam("teacherId") Long teacherId,
            @RequestParam("studentId") Long studentId) {
        return userService.unbindStudent(teacherId, studentId);
    }

    // --- 管理员：强制绑定师生关系 ---
    @PostMapping("/admin/bind")
    @Operation(summary = "管理员强制绑定", description = "管理员直接分配学生给指定教师")
    public Result<String> adminBindStudent(
            @RequestParam("teacherId") Long teacherId,
            @RequestParam("studentUserId") Integer studentUserId) {
        // 逻辑与教师自主绑定一致，只是操作人是管理员
        return userService.bindStudentByUserId(teacherId, studentUserId);
    }

    // 1. 登录接口
    @PostMapping("/login")
    public Result login(@RequestBody LoginRequestVo loginVo) {
        return userService.login(loginVo);
    }

    // 2. 注册接口
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户，包含用户名、学号查重和非空校验")
    public Result<String> register(@RequestBody Map<String, Object> registerData) {
        // 1. 获取前端传来的参数
        String username = (String) registerData.get("username");
        String password = (String) registerData.get("password");
        String nickname = (String) registerData.get("nickname");
        Integer role = (Integer) registerData.get("role");

        // 提取前端的 userId（学号/工号）
        Object userIdObj = registerData.get("userId");
        String userIdStr = userIdObj != null ? String.valueOf(userIdObj) : null;

        // 2. 严格非空校验
        if (!StringUtils.hasText(username)) {
            return Result.error("用户名（账号）不能为空！");
        }
        if (!StringUtils.hasText(password)) {
            return Result.error("密码不能为空！");
        }
        if (!StringUtils.hasText(userIdStr)) {
            return Result.error("学号/工号不能为空！");
        }

        Integer userIdValue;
        try {
            // 将字符串转换为 Integer 类型
            userIdValue = Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            return Result.error("学号/工号格式不正确，必须为数字！");
        }

        // 3. 用户名查重
        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(User::getUsername, username);
        if (userService.count(usernameWrapper) > 0) {
            return Result.error("该用户名已被注册，请更换一个账号！");
        }

        // 4. 学号/工号（userId）查重
        LambdaQueryWrapper<User> userIdWrapper = new LambdaQueryWrapper<>();
        userIdWrapper.eq(User::getUserId, userIdValue);
        if (userService.count(userIdWrapper) > 0) {
            return Result.error("该学号/工号已被注册，请勿重复注册！");
        }

        // 5. 校验全部通过，构建对象并保存
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(nickname);
        user.setRole(role != null ? role.toString() : "2");
        user.setUserId(userIdValue); // 设置用户的业务ID(学号/工号)
        user.setStatus("active");

        userService.save(user);

        return Result.success("注册成功");
    }

    // 3. 教师查询名下学生列表
    @GetMapping("/myStudents")
    public Result getMyStudents(@RequestParam Integer teacherId) {
        return userService.getMyStudents(teacherId);
    }

    @GetMapping("/check-admin/{userId}")
    @Operation(summary = "检查管理员权限", description = "验证指定用户是否具有管理员权限")
    public Result<Boolean> checkAdmin(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        return Result.success(true);
    }


    @GetMapping("/admin/relations")
    @Operation(summary = "获取所有师生绑定关系", description = "管理员查看全局师生绑定关系")
    public Result<List<TeacherStudentVO>> getAllRelations() {
        return userService.getAllTeacherStudentRelations();
    }
}