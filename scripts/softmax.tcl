# 定义项目和文件
set project_name "softmax"
set project_dir "/home/shiroha/Code/Backend/ml-accelerator/vivado" // replace with your path
set top_module "softmax_top"
set board "xcu280-fsvh2892-2L-e"


# 创建项目
create_project $project_name $project_dir -part xcu280-fsvh2892-2L-e
open_project "${project_dir}/${project_name}"

# 设置项目属性
set_property target_simulator XSim [current_project]
set_property default_lib work [current_project]

# 创建文件组并添加源文件
add_files -norecurse [glob $project_dir/*.sv]



# 综合设计
puts "Starting synthesis..."

synth_design -top $top_module -part $board 


# 设置时钟约束
reset_timing
create_clock -period 3.000 -name clock -waveform {0.000 1.500} -add [get_ports clock]


# 生成利用率报告
puts "Generating utilization report..."
report_utilization -file $project_dir/$project_name-utilization.rpt

# 生成时序报告
puts "Generating timing report..."
# report_timing_summary -file $project_dir/$project_name-timing_summary.rpt
report_timing_summary -delay_type min_max -report_unconstrained -check_timing_verbose -max_paths 10 -input_pins -routable_nets -file $project_dir/$project_name-timing_summary.rpt

# 保存项目
puts "Saving the project..."
save_project_as $project_name

# 关闭Vivado项目
puts "Closing the project..."
close_project

puts "Script completed."