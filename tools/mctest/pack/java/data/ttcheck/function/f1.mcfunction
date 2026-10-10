# [F1] ★分数未设置：全域匹配应失败 | 期望不匹配
scoreboard objectives add tt dummy
scoreboard players reset @s tt
execute if score @s tt matches -2147483648..2147483647 run scoreboard players set #f1 tt 1
execute unless score @s tt matches -2147483648..2147483647 run scoreboard players set #f1 tt 0
