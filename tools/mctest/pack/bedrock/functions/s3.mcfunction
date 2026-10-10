# [S3] 分数=3 -> 命中 | 期望匹配
scoreboard objectives add tt dummy
scoreboard players set @s tt 3
execute if entity @s[scores={tt=3}] run scoreboard players set #s3s tt 1
execute unless entity @s[scores={tt=3}] run scoreboard players set #s3s tt 0
execute if entity @s[scores={tt=!10}] run scoreboard players set #s3 tt 1
execute unless entity @s[scores={tt=!10}] run scoreboard players set #s3 tt 0
