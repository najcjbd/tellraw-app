# [F2] 分数=0 应命中 | 期望匹配
scoreboard objectives add tt dummy
scoreboard players set @s tt 0
execute if score @s tt matches 0 run scoreboard players set #f2s tt 1
execute unless score @s tt matches 0 run scoreboard players set #f2s tt 0
execute if score @s tt matches -2147483648..2147483647 run scoreboard players set #f2 tt 1
execute unless score @s tt matches -2147483648..2147483647 run scoreboard players set #f2 tt 0
