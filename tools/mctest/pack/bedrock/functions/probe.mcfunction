# 探针
say PROBE_1_CALL_CHILD
function hello
# 下面这条是**条件子命令**：看不到 PROBE_2_COND_OK 就说明 min_engine_version 太低
execute if entity @s run say PROBE_2_COND_OK
# 带选择器参数的（不依赖背包里有没有东西）
execute unless entity @s[tag=__never_set__] run say PROBE_3_SELECTOR_OK
say PROBE_4_END
