# [X7] 老式 data=0 子选项在 1.26.52 还能用吗 | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 0 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #x7s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #x7s tt 0
execute if entity @s[hasitem={item=diamond,data=0}] run scoreboard players set #x7 tt 1
execute unless entity @s[hasitem={item=diamond,data=0}] run scoreboard players set #x7 tt 0
