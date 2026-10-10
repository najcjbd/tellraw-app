# [F4] 分数=int最大值 应命中 | 期望匹配
scoreboard objectives add tt dummy
scoreboard players set @s tt 2147483647
execute if score @s tt matches 2147483647 run scoreboard players set #f4s tt 1
execute unless score @s tt matches 2147483647 run scoreboard players set #f4s tt 0
execute if score @s tt matches -2147483648..2147483647 run scoreboard players set #f4 tt 1
execute unless score @s tt matches -2147483648..2147483647 run scoreboard players set #f4 tt 0
