# [F3] 分数=int最小值 应命中 | 期望匹配
scoreboard objectives add tt dummy
scoreboard players set @s tt -2147483648
execute if score @s tt matches -2147483648 run scoreboard players set #f3s tt 1
execute unless score @s tt matches -2147483648 run scoreboard players set #f3s tt 0
execute if score @s tt matches -2147483648..2147483647 run scoreboard players set #f3 tt 1
execute unless score @s tt matches -2147483648..2147483647 run scoreboard players set #f3 tt 0
