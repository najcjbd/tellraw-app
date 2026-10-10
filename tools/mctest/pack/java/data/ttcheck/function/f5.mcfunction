# [F5] 分数=3 应命中 | 期望匹配
scoreboard objectives add tt dummy
scoreboard players set @s tt 3
execute if score @s tt matches 3 run scoreboard players set #f5s tt 1
execute unless score @s tt matches 3 run scoreboard players set #f5s tt 0
execute if score @s tt matches -2147483648..2147483647 run scoreboard players set #f5 tt 1
execute unless score @s tt matches -2147483648..2147483647 run scoreboard players set #f5 tt 0
